package com.example.algorandcarsharing.services;

import android.content.Context;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.crypto.LogicsigSignature;
import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.algorand.algosdk.v2.client.model.CompileResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.helpers.ServicesHelper;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.helpers.UtilsHelper;
import com.example.algorandcarsharing.models.AccountModel;
import com.example.algorandcarsharing.models.InsertTripModel;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.ApplicationTripSchema;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;


public class ApplicationService implements BaseService {

    protected AlgodClient client;
    protected String clientAddress = ClientConstants.algodClientAddress;
    protected String clientToken = ClientConstants.algodCClientToken;
    protected int clientPort = ClientConstants.algodCClientPort;

    public enum ProgramType  {
        ApprovalState,
        ApprovalStateTest,
        ClearState,
    }

    public ApplicationService(String clientAddress, int clientPort, String clientToken) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.clientToken = clientToken;
        this.client = this.connectToClient();
    }

    public ApplicationService() {
        this.client = this.connectToClient();
    }

    @Override
    public AlgodClient connectToClient() {
        return new AlgodClient(this.clientAddress, this.clientPort, this.clientToken);
    }

    @Override
    public AlgodClient getClient() {
        return client;
    }

    /**
     * Create a new application
     *
     * @param context
     * @param creator
     * @param tripArgs
     * @return
     */
    public Supplier<Long> createApplication(Context context, Account creator, InsertTripModel tripArgs) {
        return () -> {
                try {
                    TEALProgram approvalProgram = getCompiledProgram(context, ProgramType.ApprovalState).get();
                    TEALProgram clearStateProgram = getCompiledProgram(context, ProgramType.ClearState).get();

                    StateSchema globalState = ApplicationTripSchema.getGlobalStateSchema();
                    StateSchema localState = ApplicationTripSchema.getLocalStateSchema();

                    List<byte[]> args = tripArgs.getArgs(client);

                    // create application
                    Transaction txn = TransactionsHelper.create_txn(client, creator.getAddress(), approvalProgram, clearStateProgram, globalState, localState, args);
                    SignedTransaction signedTxn = creator.signTransaction(txn);

                    String txId = TransactionsHelper.sendTransaction(client, signedTxn);
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                    LogHelper.log(this.getClass().getName(), String.format("Created new app-id: %s", response.applicationIndex));
                    return response.applicationIndex;
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
        };
    }

    /**
     * Update an application
     *
     * @param appId
     * @param creator
     * @param tripArgs
     * @return
     */
    public Supplier<Long> updateApplication(Long appId, Account creator, InsertTripModel tripArgs) {
        return () -> {
            try {
                List<byte[]> args = new ArrayList<>();
                args.add(ApplicationTripSchema.AppMethod.UpdateTrip.getValue().getBytes(StandardCharsets.UTF_8));
                args.addAll(tripArgs.getArgs(client));

                // update application
                Transaction txn = TransactionsHelper.noop_txn(client, appId, creator.getAddress(), args);
                SignedTransaction signedTxn = creator.signTransaction(txn);

                String txId = TransactionsHelper.sendTransaction(client, signedTxn);
                PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                LogHelper.log(this.getClass().getName(), String.format("Updated app-id: %s", response.applicationIndex));
                return response.applicationIndex;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    /**
     * Delete the application and close the escrow if needed
     *
     * @param trip
     * @param creator
     * @return
     */
    public Supplier<Long> deleteApplication(TripModel trip, Account creator) {
        return () -> {
            try {
                Long appId = trip.id();
                Long amount = Long.valueOf(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost));

                // delete the trip and perform a payment from the escrow
                Transaction delete_txn = TransactionsHelper.delete_txn(client, creator.getAddress(), appId, null);
                Transaction payment_txn = TransactionsHelper.payment_txn(client, trip.escrowAddress(), creator.getAddress(), amount, creator.getAddress());

                if(!trip.isEnded()) {
                    //if trip is not already ended, close the escrow with the deletion
                    // group transactions an assign ids
                    Digest gid = TxGroup.computeGroupID(delete_txn, payment_txn);
                    delete_txn.assignGroupID(gid);
                    payment_txn.assignGroupID(gid);

                    // sign individual transactions
                    LogicsigSignature escrowSignature = getEscrowSignature(appId).get();
                    SignedTransaction delete_signedTxn = creator.signTransaction(delete_txn);
                    SignedTransaction payment_signedTxn = Account.signLogicsigTransaction(escrowSignature, payment_txn);

                    // send transactions
                    String txId = TransactionsHelper.sendTransaction(client, Arrays.asList(delete_signedTxn, payment_signedTxn));
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);
                }
                else {
                    SignedTransaction delete_signedTxn = creator.signTransaction(delete_txn);
                    String txId = TransactionsHelper.sendTransaction(client, delete_signedTxn);
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);
                }

                LogHelper.log(this.getClass().getName(), String.format("Deleted Trip for app-id: %s", appId));
                return appId;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    /**
     * Initialize the escrow
     *
     * @param appId
     * @param creator
     * @return
     */
    public Supplier<Long> initializeEscrow(Long appId, Account creator) {
        return () -> {
            try {
                // get escrow address
                LogicsigSignature escrowSignature = getEscrowSignature(appId).get();
                Address escrowAddress = escrowSignature.toAddress();

                List<byte[]> args  = new ArrayList<>();
                args.add(ApplicationTripSchema.AppMethod.InitializeEscrow.getValue().getBytes());
                args.add(escrowAddress.getBytes());

                // link the escrow to the application
                Transaction txn = TransactionsHelper.noop_txn(client, appId, creator.getAddress(), args);
                SignedTransaction signedTxn = creator.signTransaction(txn);

                String txId = TransactionsHelper.sendTransaction(client, signedTxn);
                PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                // fund escrow
                List<byte[]> args_fund  = new ArrayList<>();
                args_fund.add(ApplicationTripSchema.AppMethod.FundEscrow.getValue().getBytes());

                Transaction call_txn = TransactionsHelper.noop_txn(client, appId, creator.getAddress(), args_fund);
                Transaction payment_txn = TransactionsHelper.payment_txn(client, creator.getAddress(), escrowAddress, TransactionsHelper.escrowMinBalance, null);

                // group transactions an assign ids
                Digest gid = TxGroup.computeGroupID(call_txn, payment_txn);
                call_txn.assignGroupID(gid);
                payment_txn.assignGroupID(gid);

                // sign individual transactions
                SignedTransaction call_signedTxn = creator.signTransaction(call_txn);
                SignedTransaction payment_signedTxn = creator.signTransaction(payment_txn);

                // send transactions
                String txId_fund = TransactionsHelper.sendTransaction(client, Arrays.asList(call_signedTxn, payment_signedTxn));
                PendingTransactionResponse response_fund = TransactionsHelper.waitForConfirmation(client, txId_fund);

                LogHelper.log(this.getClass().getName(), String.format("Escrow initialized for app-id %s with address: %s", appId, escrowAddress));
                return appId;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    /**
     * Participate to the trip
     *
     * @param trip
     * @param account
     * @return
     */
    public Supplier<Long> participate(TripModel trip, AccountModel account) {
        return () -> {
            try {
                if(!trip.isValid()) {
                    throw new Exception("The application program is not valid");
                }
                Long appId = trip.id();
                Long amount = Long.valueOf(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost));

                Account sender = account.getAccount();
                ApplicationLocalState localState = account.getAppLocalState(appId);
                if(localState == null) {
                    // optin to trip if needed
                    Transaction optin_txn = TransactionsHelper.optin_txn(client, sender.getAddress(), appId, null);
                    SignedTransaction optin_signedTxn = sender.signTransaction(optin_txn);

                    String txId = TransactionsHelper.sendTransaction(client, optin_signedTxn);
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);
                    LogHelper.log(this.getClass().getName(), String.format("Opted-in to app-id: %s", appId));
                }

                // participate to the trip and perform payment to escrow
                List<byte[]> args  = new ArrayList<>();
                args.add(ApplicationTripSchema.AppMethod.Participate.getValue().getBytes());

                Transaction call_txn = TransactionsHelper.noop_txn(client, appId, sender.getAddress(), args);
                Transaction payment_txn = TransactionsHelper.payment_txn(client, sender.getAddress(), trip.escrowAddress(), amount, null);

                // group transactions an assign ids
                Digest gid = TxGroup.computeGroupID(call_txn, payment_txn);
                call_txn.assignGroupID(gid);
                payment_txn.assignGroupID(gid);

                // sign individual transactions
                SignedTransaction call_signedTxn = sender.signTransaction(call_txn);
                SignedTransaction payment_signedTxn = sender.signTransaction(payment_txn);

                // send transactions
                String txId = TransactionsHelper.sendTransaction(client, Arrays.asList(call_signedTxn, payment_signedTxn));
                PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                LogHelper.log(this.getClass().getName(), String.format("Participated to app-id: %s", appId));
                return appId;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    /**
     * Cancel the participation to the trip
     *
     * @param trip
     * @param account
     * @return
     */
    public Supplier<Long> cancelParticipation(TripModel trip, AccountModel account) {
        return () -> {
            try {
                if(!trip.isValid()) {
                    throw new Exception("The application program is not valid");
                }
                Long appId = trip.id();
                Long amount = Long.valueOf(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost));

                Account sender = account.getAccount();
                ApplicationLocalState localState = account.getAppLocalState(appId);
                if(localState == null) {
                    // optin to trip if needed
                    Transaction optin_txn = TransactionsHelper.optin_txn(client, sender.getAddress(), appId, null);
                    SignedTransaction optin_signedTxn = sender.signTransaction(optin_txn);

                    String txId = TransactionsHelper.sendTransaction(client, optin_signedTxn);
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);
                    LogHelper.log(this.getClass().getName(), String.format("Opted-in to app-id: %s", appId));
                }

                // cancel participation to the trip and perform refund
                List<byte[]> args  = new ArrayList<>();
                args.add(ApplicationTripSchema.AppMethod.CancelParticipation.getValue().getBytes());

                Transaction call_txn = TransactionsHelper.noop_txn(client, appId, sender.getAddress(), args);
                Transaction payment_txn = TransactionsHelper.payment_txn(client, trip.escrowAddress(), sender.getAddress(), amount, null);

                // group transactions an assign ids
                Digest gid = TxGroup.computeGroupID(call_txn, payment_txn);
                call_txn.assignGroupID(gid);
                payment_txn.assignGroupID(gid);

                // sign individual transactions
                LogicsigSignature escrowSignature = getEscrowSignature(appId).get();
                SignedTransaction call_signedTxn = sender.signTransaction(call_txn);
                SignedTransaction payment_signedTxn = Account.signLogicsigTransaction(escrowSignature, payment_txn);

                // send transactions
                String txId = TransactionsHelper.sendTransaction(client, Arrays.asList(call_signedTxn, payment_signedTxn));
                PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                LogHelper.log(this.getClass().getName(), String.format("Cancelled participation to app-id: %s", appId));
                return appId;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    /**
     * End the trip by starting it and closing the escrow.
     *
     * @param trip
     * @param creator
     * @return
     */
    public Supplier<Long> startTrip(TripModel trip, Account creator) {
        return () -> {
            try {
                Long appId = trip.id();
                Long amount = Long.valueOf(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost));

                // participate to the trip and perform a payment from the escrow
                List<byte[]> args  = new ArrayList<>();
                args.add(ApplicationTripSchema.AppMethod.StartTrip.getValue().getBytes());

                Transaction call_txn = TransactionsHelper.noop_txn(client, appId, creator.getAddress(), args);
                Transaction payment_txn = TransactionsHelper.payment_txn(client, trip.escrowAddress(), creator.getAddress(), amount, creator.getAddress());

                // group transactions an assign ids
                Digest gid = TxGroup.computeGroupID(call_txn, payment_txn);
                call_txn.assignGroupID(gid);
                payment_txn.assignGroupID(gid);

                // sign individual transactions
                LogicsigSignature escrowSignature = getEscrowSignature(appId).get();
                SignedTransaction call_signedTxn = creator.signTransaction(call_txn);
                SignedTransaction payment_signedTxn = Account.signLogicsigTransaction(escrowSignature, payment_txn);

                // send transactions
                String txId = TransactionsHelper.sendTransaction(client, Arrays.asList(call_signedTxn, payment_signedTxn));
                PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                LogHelper.log(this.getClass().getName(), String.format("Started Trip for app-id: %s", appId));
                return appId;
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    protected Future<LogicsigSignature> getEscrowSignature(Long appId) {
        return new Future<LogicsigSignature>() {

            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public LogicsigSignature get() {
                try {
                    String programSource = "#pragma version 4\n" +
                            "global GroupSize\n" +
                            "int 2\n" +
                            "==\n" +
                            "assert\n" +
                            "gtxn 0 ApplicationID\n" +
                            "int "+ appId +"\n" +
                            "==\n" +
                            "assert\n" +
                            "gtxn 1 TypeEnum\n" +
                            "int pay\n" +
                            "==\n" +
                            "assert\n" +
                            "int 1\n" +
                            "return";

                    Response<CompileResponse> response = client.TealCompile().source(programSource.getBytes(StandardCharsets.UTF_8)).execute();
                    ServicesHelper.checkResponse(response);

                    CompileResponse programCompiled = response.body();
                    byte[] program = Base64.getDecoder().decode(programCompiled.result);

                    return new LogicsigSignature(program, null);
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public LogicsigSignature get(long l, TimeUnit timeUnit) {
                return this.get();
            }
        };
    }

    protected Future<TEALProgram> getCompiledProgram(Context context, ProgramType programType) {
        return new Future<TEALProgram>() {

            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public TEALProgram get() {
                try {
                    String programSource = "";
                    switch (programType) {
                        case ApprovalState: programSource = "contracts/carsharing_approval.teal"; break;
                        case ApprovalStateTest: programSource = "contracts/carsharing_approval_test.teal"; break;
                        case ClearState: programSource = "contracts/carsharing_clear_state.teal"; break;
                    }

                    String program = UtilsHelper.readAssetFile(context, programSource);
                    Response<CompileResponse> response = client.TealCompile().source(program.getBytes(StandardCharsets.UTF_8)).execute();
                    ServicesHelper.checkResponse(response);

                    CompileResponse programCompiled = response.body();
                    return new TEALProgram(programCompiled.result);
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public TEALProgram get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                return this.get();
            }
        };
    }


}
