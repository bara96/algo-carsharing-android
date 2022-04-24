package com.example.algorandcarsharing.services;

import com.algorand.algosdk.v2.client.common.Client;
import com.algorand.algosdk.v2.client.common.Response;

public abstract class BaseService implements ServiceInterface {
    @Override
    public Client connectToClient() {
        return null;
    }

    @Override
    public Client getClient() {
        return null;
    }

    @Override
    public void checkResponse(Response response) throws Exception {
        if (!response.isSuccessful()) {
            String message = "Response code: "
                    .concat(String.valueOf(response.code()))
                    .concat(", with message: ")
                    .concat(response.message());
            throw new Exception(message);
        }
    }
}
