package com.example.algorandcarsharing.services;

import android.content.Context;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.CompileResponse;
import com.algorand.algosdk.v2.client.model.NodeStatusResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.helpers.ServicesHelper;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.helpers.UtilsHelper;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.CreateTripModel;

import java.nio.charset.StandardCharsets;
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
    protected String transactionNote;

    public enum ProgramType  {
        ApprovalState,
        ClearState,
    }

    public ApplicationService(String clientAddress, int clientPort, String clientToken) {
        this.transactionNote = ApplicationConstants.transactionNote;

        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.clientToken = clientToken;
        this.client = this.connectToClient();
    }

    public ApplicationService() {
        this.transactionNote = ApplicationConstants.transactionNote;;

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

    public Supplier<Long> createApplication(Context context, Account sender, CreateTripModel tripArgs) {
        return () -> {
                try {
                    TEALProgram approvalProgram = getCompiledProgram(context, ProgramType.ApprovalState).get();
                    TEALProgram clearStateProgram = getCompiledProgram(context, ProgramType.ClearState).get();

                    StateSchema globalState = TripModel.getGlobalStateSchema();
                    StateSchema localState = TripModel.getLocalStateSchema();

                    List<byte[]> args = tripArgs.getArgs(client);

                    // create txn
                    Transaction txn = TransactionsHelper.create_txn(client, sender, approvalProgram, clearStateProgram, globalState, localState, args);
                    SignedTransaction signedTxn = sender.signTransaction(txn);

                    // send txn
                    String txId = TransactionsHelper.sendTransaction(client, signedTxn);

                    // wait for txn confirmation
                    PendingTransactionResponse response = TransactionsHelper.waitForConfirmation(client, txId);

                    LogHelper.log(this.getClass().getName(), String.format("Created new app-id: %s", response.applicationIndex));
                    return response.applicationIndex;
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
        };
    }

    protected Future<TEALProgram> getCompiledEscrow(String appId) {
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
                    String program = "#pragma version 5\n" +
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
            public TEALProgram get(long l, TimeUnit timeUnit) {
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
            public TEALProgram get() throws ExecutionException, InterruptedException {
                try {
                    String programFile = "";
                    switch (programType) {
                        case ApprovalState: programFile = "contracts/carsharing_approval.teal"; break;
                        case ClearState: programFile = "contracts/carsharing_clear_state.teal"; break;
                    }

                    String program = UtilsHelper.readAssetFile(context, programFile);
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
