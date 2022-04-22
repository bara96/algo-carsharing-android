package com.example.algorandcarsharing.helpers;

import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;

public class TransactionsHelper {

    protected final Long fees = 1000L;
    protected final Long escrowMinBalance = 1000000L;

    public static void create_txn(AlgodClient client, Account account) throws Exception {
        Response< TransactionParametersResponse > resp = client.TransactionParams().execute();
        if (!resp.isSuccessful()) {
            throw new Exception(resp.message());
        }
        TransactionParametersResponse params = resp.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }
    }
}
