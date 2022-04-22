package com.example.algorandcarsharing.services;

import android.content.Context;
import android.util.Log;

import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.v2.client.algod.GetStatus;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.CompileResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.constants.Constants;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.helpers.UtilitiesHelper;
import com.example.algorandcarsharing.models.BaseTripModel;
import com.example.algorandcarsharing.models.CreateTripModel;

import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    protected final Long maxWaitingTime = 30000L;
    protected boolean showLogs = true;

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

    /**
     * Connect to indexer Client
     * @return Client
     */
    public AlgodClient connectToClient() {
        return new AlgodClient(this.clientAddress, this.clientPort, this.clientToken);
    }

    public AlgodClient getClient() {
        return client;
    }

    protected TEALProgram getCompiledProgram(Context context, ProgramType programType) {
        String programFile = "";
        switch (programType) {
            case ApprovalState: programFile = "contracts/carsharing_approval.teal"; break;
            case ClearState: programFile = "contracts/carsharing_clear_state.teal"; break;
        }
        try {
            String program = UtilitiesHelper.readAssetFile(context, programFile);
            Response<CompileResponse> response = client.TealCompile().source(program.getBytes(StandardCharsets.UTF_8)).execute();
            CompileResponse programCompiled = response.body();
            return new TEALProgram(programCompiled.result);
        }
        catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage());
            throw new CompletionException(e);
        }
    }

    public Supplier<Long> createApplication(Context context, Account sender, CreateTripModel tripArgs) {
        return new Supplier<Long>() {
            @Override
            public Long get() {
                try {
                    TEALProgram approvalProgram = getCompiledProgram(context, ProgramType.ApprovalState);
                    TEALProgram clearStateProgram = getCompiledProgram(context, ProgramType.ClearState);

                    StateSchema globalState = BaseTripModel.getGlobalStateSchema();
                    StateSchema localState = BaseTripModel.getLocalStateSchema();

                    List<byte[]> args = tripArgs.getArgs(client);

                    Transaction txn = TransactionsHelper.create_txn(client, sender, approvalProgram, clearStateProgram, globalState, localState, args);
                    SignedTransaction signedTxn = sender.signTransaction(txn);

                    PendingTransactionResponse response = waitForConfirmation(signedTxn.transactionID).get();
                    if(showLogs) {
                        Log.d(this.getClass().getName(), String.format("Created new app-id: %s", response.applicationIndex));
                    }
                    return response.applicationIndex;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
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
            public PendingTransactionResponse get() throws ExecutionException, InterruptedException {
                try {
                    long startTime = System.currentTimeMillis(); //fetch starting time
                    Long lastRound = client.GetStatus().execute().body().lastRound;
                    while ((System.currentTimeMillis()-startTime) < maxWaitingTime) {
                        // Check the pending transactions
                        Response<PendingTransactionResponse> pendingInfo = client.PendingTransactionInformation(txID).execute();
                        if (pendingInfo.body() != null && pendingInfo.body().confirmedRound != null && pendingInfo.body().confirmedRound > 0) {
                            // Got the completed Transaction
                            if (showLogs) {
                                Log.d(this.getClass().getName(), "Transaction " + txID + " confirmed in round " + pendingInfo.body().confirmedRound);
                            }
                            return pendingInfo.body();
                        }
                        lastRound++;
                        client.WaitForBlock(lastRound).execute();
                    }
                    throw new Exception("Timeout on waiting for confirmation");
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public PendingTransactionResponse get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                return this.get();
            }
        };
    }

}
