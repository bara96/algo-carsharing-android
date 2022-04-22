package com.example.algorandcarsharing.services;

import android.util.Log;

import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.ApplicationResponse;
import com.algorand.algosdk.v2.client.model.Enums;
import com.algorand.algosdk.v2.client.model.TransactionsResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class IndexerService implements BaseService {

    protected IndexerClient client;
    protected String clientAddress = ClientConstants.indexerClientAddress;
    protected int clientPort = ClientConstants.indexerClientPort;
    protected String transactionNote;

    protected boolean showLogs = true;

    public IndexerService(String clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.client = this.connectToClient();
        this.transactionNote = ApplicationConstants.transactionNote;
    }

    public IndexerService() {
        this.transactionNote = ApplicationConstants.transactionNote;
        this.client = this.connectToClient();
    }

    /**
     * Connect to indexer Client
     * @return Client
     */
    public IndexerClient connectToClient() {
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
