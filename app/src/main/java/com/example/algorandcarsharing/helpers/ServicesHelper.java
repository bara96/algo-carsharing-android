package com.example.algorandcarsharing.helpers;

import com.algorand.algosdk.v2.client.common.Response;

public class ServicesHelper {
    /**
     * Check if the response is valid
     *
     * @param response
     * @throws Exception
     */
    public static void checkResponse(Response response) throws Exception {
        if (!response.isSuccessful()) {
            String message = "Response code: "
                    .concat(String.valueOf(response.code()))
                    .concat(", with message: ")
                    .concat(response.message());
            throw new Exception(message);
        }
    }
}
