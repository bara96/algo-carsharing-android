package com.example.algorandcarsharing.services;

import android.content.Context;
import android.util.Log;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Account;
import com.example.algorandcarsharing.R;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class ApplicationService {

    protected Context context;
    protected AlgodClient client;
    protected String clientAddress = "10.0.2.2";
    protected String clientToken = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    protected int clientPort = 4001;
    protected String transactionNote;

    protected boolean showLogs = true;

    public ApplicationService(Context context, String clientAddress, int clientPort, String clientToken) {
        this.context = context;
        this.transactionNote = context.getString(R.string.preferences_account);

        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.clientToken = clientToken;
        this.client = this.connectToClient();
    }

    public ApplicationService(Context context) {
        this.context = context;
        this.transactionNote = context.getString(R.string.preferences_account);

        this.client = this.connectToClient();
    }

    /**
     * Connect to indexer Client
     * @return Client
     */
    private AlgodClient connectToClient() {
        return new AlgodClient(this.clientAddress, this.clientPort, this.clientToken);
    }

    public AlgodClient getClient() {
        return client;
    }

    public Supplier<Long> getBalance(String address) {
        return new Supplier<Long>() {
            @Override
            public Long get() {
                try {
                    Address pk = new Address(address);

                    Response<Account> respAcct = client.AccountInformation(pk).execute();
                    if (!respAcct.isSuccessful()) {
                        throw new Exception(respAcct.message());
                    }
                    Account accountInfo = respAcct.body();
                    if(showLogs) {
                        Log.d(this.getClass().getName(), respAcct.toString());
                    }
                    return accountInfo.amount;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
            }
        };
    }
}
