package com.example.algorandcarsharing.services;

import android.util.Log;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Account;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class AccountService implements BaseService {

    protected AlgodClient client;
    protected String clientAddress = ClientConstants.algodClientAddress;
    protected String clientToken = ClientConstants.algodCClientToken;
    protected int clientPort = ClientConstants.algodCClientPort;
    protected String transactionNote;

    protected boolean showLogs = true;

    public AccountService(String clientAddress, int clientPort, String clientToken) {
        this.transactionNote = ApplicationConstants.transactionNote;;

        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.clientToken = clientToken;
        this.client = this.connectToClient();
    }

    public AccountService() {
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

    public Supplier<Account> getAccountInfo(String address) {
        return new Supplier<Account>() {
            @Override
            public Account get() {
                try {
                    Address pk = new Address(address);

                    Response<Account> response = client.AccountInformation(pk).execute();
                    if (!response.isSuccessful()) {
                        throw new Exception(response.message());
                    }
                    Account accountInfo = response.body();
                    if(showLogs) {
                        Log.d(this.getClass().getName(), response.toString());
                    }
                    return accountInfo;
                }
                catch (Exception e) {
                    Log.e(this.getClass().getName(), e.getMessage());
                    throw new CompletionException(e);
                }
            }
        };
    }
}
