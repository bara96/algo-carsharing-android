package com.example.algorandcarsharing.services;

import com.algorand.algosdk.v2.client.common.Client;

public interface BaseService {
    public Client connectToClient();

    public Client getClient();
}
