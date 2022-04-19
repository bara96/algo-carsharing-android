package com.example.algorandcarsharing.helpers;

import android.content.Context;

import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Enums;
import com.algorand.algosdk.v2.client.model.TransactionsResponse;
import com.example.algorandcarsharing.R;

import org.json.JSONObject;


public class IndexerHelper {

    protected Context context;
    protected IndexerClient indexerClient;
    protected String indexerAddress = "10.0.2.2";
    protected int indexerPort = 8980;
    protected String transactionNote;

    public IndexerHelper(Context context, String indexerAddress, int indexerPort) {
        this.indexerAddress = indexerAddress;
        this.indexerPort = indexerPort;
        this.indexerClient = this.connectToClient();
        this.context = context;
        this.transactionNote = context.getString(R.string.preferences_account);
    }

    public IndexerHelper(Context context) {
        this.context = context;
        this.transactionNote = context.getString(R.string.preferences_account);
        this.indexerClient = this.connectToClient();
    }

    /**
     * Connect to indexer Client
     * @return Client
     */
    private IndexerClient connectToClient() {
        return new IndexerClient(this.indexerAddress, this.indexerPort);
    }

    public IndexerClient getIndexerClient() {
        return indexerClient;
    }

    public JSONObject searchTransactions() throws Exception {
        Response<TransactionsResponse> response = this.indexerClient
                .searchForTransactions()
                .notePrefix(this.transactionNote.getBytes())
                .txType(Enums.TxType.APPL)
                .limit(100L)
                .execute();

        if(!response.isSuccessful()) {
            String message = "Response code: "
                    .concat(String.valueOf(response.code()))
                    .concat(", with message: ")
                    .concat(response.message());
            throw new Exception(message);
        }

        return new JSONObject(String.valueOf(response.body()));
    }
}
