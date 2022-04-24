package com.example.algorandcarsharing.services;

import com.algorand.algosdk.v2.client.common.Client;
import com.algorand.algosdk.v2.client.common.Response;

public interface BaseService {
    public Client connectToClient();

    public Client getClient();
}
