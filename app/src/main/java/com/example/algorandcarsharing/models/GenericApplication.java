package com.example.algorandcarsharing.models;

import com.algorand.algosdk.v2.client.common.PathResponse;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.algorand.algosdk.v2.client.model.Transaction;

public class GenericApplication extends PathResponse {

    public static <T> ApplicationLocalState applicationLocalState(T response) {
        return (ApplicationLocalState) response;
    }

    public static <T> Application application(T response) {
        return (Application) response;
    }

    public static <T> Transaction transaction(T response) {
        return (Transaction) response;
    }
}