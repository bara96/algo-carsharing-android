package com.example.algorandcarsharing.helpers;

import android.util.Log;

import com.algorand.algosdk.crypto.TEALProgram;
import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.example.algorandcarsharing.constants.ClientConstants;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public class TransactionsHelper {

    protected static final Long fees = 1000L;
    protected static final Long escrowMinBalance = 1000000L;

    public TransactionsHelper() {
    }

    public static SignedTransaction signedTransaction(Transaction transaction, Account account) throws NoSuchAlgorithmException {
        SignedTransaction signedTxn = account.signTransaction(transaction);
        Log.d(TransactionsHelper.class.getName(),"Signed transaction with txid: " + signedTxn.transactionID);
        return signedTxn;
    }

    public static Transaction create_txn(AlgodClient client, Account sender, TEALProgram approvalProgram, TEALProgram clearStateProgram, StateSchema globalStateSchema, StateSchema localStateSchema, List<byte[]> args) throws Exception {
        String transactionNote = ClientConstants.indexerClientAddress;
        Response< TransactionParametersResponse > resp = client.TransactionParams().execute();
        if (!resp.isSuccessful()) {
            throw new Exception(resp.message());
        }
        TransactionParametersResponse params = resp.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }

        return Transaction.ApplicationCreateTransactionBuilder()
                .suggestedParams(params)
                .note(transactionNote.getBytes())
                .sender(sender.getAddress())
                .approvalProgram(approvalProgram)
                .clearStateProgram(clearStateProgram)
                .globalStateSchema(globalStateSchema)
                .localStateSchema(localStateSchema)
                .args(args)
                .build();
    }
}
