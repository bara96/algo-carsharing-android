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


public class ApplicationService extends BaseService {

    protected AlgodClient client;
    protected String clientAddress = ClientConstants.algodClientAddress;
    protected String clientToken = ClientConstants.algodCClientToken;
    protected int clientPort = ClientConstants.algodCClientPort;
    protected String transactionNote;

    protected final Long maxWaitingRounds = 1000L;

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
                    String txId = sendTransaction(signedTxn).get();

                    // wait for txn confirmation
                    PendingTransactionResponse response = waitForConfirmation(txId).get();

                    LogHelper.log(this.getClass().getName(), String.format("Created new app-id: %s", response.applicationIndex));
                    return response.applicationIndex;
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
        };
    }

    public Future<TEALProgram> getCompiledProgram(Context context, ProgramType programType) {
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
                    checkResponse(response);

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

    public Future<String> sendTransaction(SignedTransaction txn) {
        return new Future<String>() {
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
            public String get() {
                try {
                    // Submit the transaction to the network
                    byte[] encodedTxBytes = Encoder.encodeToMsgPack(txn);
                    Response<PostTransactionsResponse> response = client.RawTransaction().rawtxn(encodedTxBytes).execute();
                    checkResponse(response);

                    String txID = response.body().txId;
                    LogHelper.log(this.getClass().getName(), "Sent transaction with ID: " + txID);
                    return txID;
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public String get(long l, TimeUnit timeUnit) {
                return this.get();
            }
        };
    }

    public Future<PendingTransactionResponse> waitForConfirmation(String txID) {
        return new Future<PendingTransactionResponse>() {
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
            public PendingTransactionResponse get() {
                try {
                    Response<NodeStatusResponse> statusResponse = client.GetStatus().execute();
                    checkResponse(statusResponse);

                    long lastRound = statusResponse.body().lastRound;
                    long currentRound = lastRound + 1;
                    while (true) {
                        if(currentRound > lastRound + maxWaitingRounds) {
                            throw new Exception("Timeout on waiting for confirmation");
                        }

                        try {
                            // Check the pending transactions
                            Response<PendingTransactionResponse> pendingInfoResponse = client.PendingTransactionInformation(txID).execute();
                            checkResponse(pendingInfoResponse);

                            if (pendingInfoResponse.body().confirmedRound != null && pendingInfoResponse.body().confirmedRound > 0) {
                                // Got the completed Transaction
                                LogHelper.log(this.getClass().getName(), "Transaction " + txID + " confirmed in round " + pendingInfoResponse.body().confirmedRound);
                                return pendingInfoResponse.body();
                            }
                            client.WaitForBlock(currentRound).execute();
                            currentRound++;
                        }
                        catch (Exception e) {
                            LogHelper.error("waitForConfirmation txn: " + txID, e, false);
                        }
                    }
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public PendingTransactionResponse get(long l, TimeUnit timeUnit) {
                return this.get();
            }
        };
    }
}
