package com.example.algorandcarsharing.services;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Account;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.ClientConstants;
import com.example.algorandcarsharing.helpers.ServicesHelper;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;


public class AccountService implements BaseService {

    protected AlgodClient client;
    protected String clientAddress = ClientConstants.algodClientAddress;
    protected String clientToken = ClientConstants.algodCClientToken;
    protected int clientPort = ClientConstants.algodCClientPort;
    protected String transactionNote;

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

    @Override
    public AlgodClient connectToClient() {
        return new AlgodClient(this.clientAddress, this.clientPort, this.clientToken);
    }

    @Override
    public AlgodClient getClient() {
        return client;
    }

    public Supplier<Account> getAccountInfo(String address) {
        return () -> {
            try {
                Address pk = new Address(address);

                Response<Account> response = client.AccountInformation(pk).execute();
                ServicesHelper.checkResponse(response);

                return response.body();
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }
}
