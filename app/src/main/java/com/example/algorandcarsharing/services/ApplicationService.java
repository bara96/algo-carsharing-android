package com.example.algorandcarsharing.services;

import android.content.Context;
import android.util.Log;

import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.CompileResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.helpers.UtilitiesHelper;
import com.example.algorandcarsharing.models.BaseTripModel;
import com.example.algorandcarsharing.models.CreateTripModel;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class ApplicationService implements BaseService {

    protected AlgodClient client;
    protected String clientAddress = ClientConstants.algodClientAddress;
    protected String clientToken = ClientConstants.algodCClientToken;
    protected int clientPort = ClientConstants.algodCClientPort;
    protected String transactionNote;

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

    public Supplier<Transaction> createApplication(Context context, Account sender, CreateTripModel tripArgs) {
        return new Supplier<Transaction>() {
            @Override
            public Transaction get() {
                try {
                    TEALProgram approvalProgram = getCompiledProgram(context, ProgramType.ApprovalState);
                    TEALProgram clearStateProgram = getCompiledProgram(context, ProgramType.ClearState);

                    StateSchema globalState = BaseTripModel.getGlobalStateSchema();
                    StateSchema localState = BaseTripModel.getLocalStateSchema();

                    List<byte[]> args = tripArgs.getArgs(client);

                    Transaction txn = TransactionsHelper.create_txn(client, sender, approvalProgram, clearStateProgram, globalState, localState, args);
                    return txn;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
            }
        };
    }

}
