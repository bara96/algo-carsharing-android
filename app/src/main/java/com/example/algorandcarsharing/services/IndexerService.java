package com.example.algorandcarsharing.services;

import android.content.Context;
import android.util.Log;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.indexer.SearchForApplications;
import com.algorand.algosdk.v2.client.model.Account;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationResponse;
import com.algorand.algosdk.v2.client.model.Enums;
import com.algorand.algosdk.v2.client.model.TransactionsResponse;
import com.example.algorandcarsharing.R;

import org.json.JSONObject;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class IndexerService {

    protected Context context;
    protected IndexerClient client;
    protected String clientAddress = "10.0.2.2";
    protected int clientPort = 8980;
    protected String transactionNote;

    protected boolean showLogs = true;

    public IndexerService(Context context, String clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.client = this.connectToClient();
        this.context = context;
        this.transactionNote = context.getString(R.string.env_transaction_note);
    }

    public IndexerService(Context context) {
        this.context = context;
        this.transactionNote = context.getString(R.string.env_transaction_note);
        this.client = this.connectToClient();
    }

    /**
     * Connect to indexer Client
     * @return Client
     */
    private IndexerClient connectToClient() {
        return new IndexerClient(this.clientAddress, this.clientPort);
    }

    public IndexerClient getClient() {
        return client;
    }

    public Supplier<TransactionsResponse> getTransactions() {
        return new Supplier<TransactionsResponse>() {
            @Override
            public TransactionsResponse get() {
                try {
                    Response<TransactionsResponse> response = client.searchForTransactions()
                            .notePrefix(transactionNote.getBytes())
                            .txType(Enums.TxType.APPL)
                            .limit(100L)
                            .execute();

                    if (!response.isSuccessful()) {
                        String message = "Response code: "
                                .concat(String.valueOf(response.code()))
                                .concat(", with message: ")
                                .concat(response.message());
                        throw new Exception(message);
                    }
                    TransactionsResponse transactions = response.body();
                    if(showLogs) {
                        Log.d(this.getClass().getName(), response.toString());
                    }
                    return transactions;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
            }
        };
    }

    public Supplier<ApplicationResponse> getApplication(Long appid) {
        return new Supplier<ApplicationResponse>() {
            @Override
            public ApplicationResponse get() {
                try {
                    Response<ApplicationResponse> response = client.lookupApplicationByID(appid)
                            .execute();

                    if (!response.isSuccessful()) {
                        String message = "Response code: "
                                .concat(String.valueOf(response.code()))
                                .concat(", with message: ")
                                .concat(response.message());
                        throw new Exception(message);
                    }
                    ApplicationResponse application = response.body();
                    if(showLogs) {
                        Log.d(this.getClass().getName(), response.toString());
                    }
                    return application;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
            }
        };
    }
}
