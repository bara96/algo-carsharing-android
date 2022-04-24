package com.example.algorandcarsharing.services;

import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.ApplicationResponse;
import com.algorand.algosdk.v2.client.model.Enums;
import com.algorand.algosdk.v2.client.model.TransactionsResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.helpers.ServicesHelper;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class IndexerService implements BaseService {

    protected IndexerClient client;
    protected String clientAddress = ClientConstants.indexerClientAddress;
    protected int clientPort = ClientConstants.indexerClientPort;
    protected String transactionNote;

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

    @Override
    public IndexerClient connectToClient() {
        return new IndexerClient(this.clientAddress, this.clientPort);
    }

    @Override
    public IndexerClient getClient() {
        return client;
    }

    public Supplier<TransactionsResponse> getTransactions() {
        return () -> {
            try {
                Response<TransactionsResponse> response = client.searchForTransactions()
                        .notePrefix(transactionNote.getBytes())
                        .txType(Enums.TxType.APPL)
                        .limit(100L)
                        .execute();
                ServicesHelper.checkResponse(response);

                return response.body();
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    public Supplier<ApplicationResponse> getApplication(Long appid) {
        return () -> {
            try {
                Response<ApplicationResponse> response = client.lookupApplicationByID(appid).execute();
                ServicesHelper.checkResponse(response);

                return response.body();
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }
}
