package com.example.algorandcarsharing.helpers;

import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationResponse;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.Constants;

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

    /**
     * Check if the application is trusted
     *
     * @param app
     * @return
     */
    public static boolean isTrustedApplication(Application app) {
        String approvalProgram = app.params.approvalProgram();
        String clearStateProgram = app.params.clearStateProgram();

        if(!approvalProgram.equals(ApplicationConstants.approvalProgramHash)) {
            if(Constants.development) {
                if(!approvalProgram.equals(ApplicationConstants.approvalProgramHashTest)) {
                    return false;
                }
            }
            else return false;

        }
        if(!clearStateProgram.equals(ApplicationConstants.clearStateProgramHash)) {
            return false;
        }
        return true;
    }
}
