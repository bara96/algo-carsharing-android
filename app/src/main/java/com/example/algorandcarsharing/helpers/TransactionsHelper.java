package com.example.algorandcarsharing.helpers;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.NodeStatusResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public class TransactionsHelper {

    public static final Long escrowMinBalance = 1000000L;
    public static final Long maxWaitingRounds = 1000L;

    public static SignedTransaction signTransaction(Transaction transaction, Account account) throws NoSuchAlgorithmException {
        return account.signTransaction(transaction);
    }

    public static String sendTransaction(AlgodClient client, SignedTransaction txn) throws Exception {
        // Submit the transaction to the network
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(txn);
        Response<PostTransactionsResponse> response = client.RawTransaction().rawtxn(encodedTxBytes).execute();
        ServicesHelper.checkResponse(response);

        String txID = response.body().txId;
        LogHelper.log(TransactionsHelper.class.getName(), "Sent transaction with ID: " + txID);
        return txID;
    }

    public static PendingTransactionResponse waitForConfirmation(AlgodClient client, String txID) throws Exception {
        Response<NodeStatusResponse> statusResponse = client.GetStatus().execute();
        ServicesHelper.checkResponse(statusResponse);

        long lastRound = statusResponse.body().lastRound;
        long currentRound = lastRound + 1;
        while (true) {
            if(currentRound > lastRound + maxWaitingRounds) {
                throw new Exception("Timeout on waiting for confirmation");
            }

            try {
                // Check the pending transactions
                Response<PendingTransactionResponse> pendingInfoResponse = client.PendingTransactionInformation(txID).execute();
                ServicesHelper.checkResponse(pendingInfoResponse);

                if (pendingInfoResponse.body().confirmedRound != null && pendingInfoResponse.body().confirmedRound > 0) {
                    // Got the completed Transaction
                    LogHelper.log(TransactionsHelper.class.getName(), "Transaction " + txID + " confirmed in round " + pendingInfoResponse.body().confirmedRound);
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

    public static Transaction create_txn(AlgodClient client, Address sender, TEALProgram approvalProgram, TEALProgram clearStateProgram, StateSchema globalStateSchema, StateSchema localStateSchema, List<byte[]> args) throws Exception {
        String transactionNote = ApplicationConstants.transactionNote;
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationCreateTransactionBuilder()
                .suggestedParams(params)
                .note(transactionNote.getBytes())
                .sender(sender)
                .approvalProgram(approvalProgram)
                .clearStateProgram(clearStateProgram)
                .globalStateSchema(globalStateSchema)
                .localStateSchema(localStateSchema)
                .args(args)
                .build();
    }

    public static Transaction update_txn(AlgodClient client, Address sender, Long appId, TEALProgram approvalProgram, TEALProgram clearStateProgram, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationUpdateTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .applicationId(appId)
                .approvalProgram(approvalProgram)
                .clearStateProgram(clearStateProgram)
                .args(args)
                .build();
    }

    public static Transaction delete_txn(AlgodClient client, Address sender, Long appId, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationDeleteTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .applicationId(appId)
                .args(args)
                .build();
    }

    public static Transaction clear_txn(AlgodClient client, Address sender, Long appId, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationClearTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .applicationId(appId)
                .args(args)
                .build();
    }

    public static Transaction close_out_txn(AlgodClient client, Address sender, Long appId, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationCloseTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .applicationId(appId)
                .args(args)
                .build();
    }

    public static Transaction optin_txn(AlgodClient client, Address sender, Long appId, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationOptInTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .applicationId(appId)
                .args(args)
                .build();
    }

    public static Transaction noop_txn(AlgodClient client, Long appId, Address sender, List<byte[]> args) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationCallTransactionBuilder()
                .suggestedParams(params)
                .applicationId(appId)
                .sender(sender)
                .args(args)
                .build();
    }

    public static Transaction payment_txn(AlgodClient client, Address sender, Address receiver, Long amount) throws Exception {
        Response<TransactionParametersResponse> response = client.TransactionParams().execute();
        ServicesHelper.checkResponse(response);

        TransactionParametersResponse params = response.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.PaymentTransactionBuilder()
                .suggestedParams(params)
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .build();
    }
}
