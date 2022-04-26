package com.example.algorandcarsharing.models;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.algorand.algosdk.v2.client.model.TealKeyValue;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.Constants;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class TripModel implements TripSchema {

    protected Application application;
    protected HashMap<String, String> globalState = new HashMap<>();
    protected HashMap<String, String> localState = new HashMap<>();

    public TripModel(Application application) {
        this.application = application;
        this.globalState = readGlobalState(application);
    }

    /**
     *
     * @return the Application id
     */
    public Long id() {
        return this.application.id;
    }

    /**
     *
     * @return the Application content
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Check if the application is trusted
     *
     * @return true if the application is trusted, false otherwise
     */
    public boolean isValid() {
        String approvalProgram = this.application.params.approvalProgram();
        String clearStateProgram = this.application.params.clearStateProgram();

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

    /**
     * Check if the application content is empty
     *
     * @return true if the application is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.application == null || this.application.id == null;
    }

    public String getGlobalStateKey(GlobalState key) {
        return this.globalState.getOrDefault(key.getValue(), null);
    }

    public String getLocalStateKey(GlobalState key) {
        return this.localState.getOrDefault(key.getValue(), null);
    }

    public HashMap<String, String> getGlobalState() {
        return globalState;
    }

    public void setGlobalState(HashMap<String, String> globalState) {
        this.globalState = globalState;
    }

    public HashMap<String, String> getLocalState() {
        return localState;
    }

    public void setLocalState(HashMap<String, String> localState) {
        this.localState = localState;
    }

    /**
     * Read the LocalState StateSchema of the application
     *
     * @param application
     * @return an hashmap with the key, value of the StateSchema
     */
    public static HashMap<String, String> readLocalState(ApplicationLocalState application) {
        if(application != null) {
            return readState(application.keyValue);
        }
        else
            return new HashMap<>();
    }

    /**
     * Read the GlobalState StateSchema of the application
     *
     * @param application
     * @return an hashmap with the key, value of the StateSchema
     */
    public static HashMap<String, String> readGlobalState(Application application) {
        if(application.params != null && application.params.globalState != null) {
            return readState(application.params.globalState);
        }
        else
            return new HashMap<>();
    }

    /**
     * Parse a StateSchema
     *
     * @param stateRaw
     * @return an hashmap with the <key, value> of the StateSchema
     */
    public static HashMap<String, String> readState(List<TealKeyValue> stateRaw) {
        HashMap<String, String> globalState = new HashMap<>();

        for(int i=0; i < stateRaw.size(); i++) {
            TealKeyValue state = stateRaw.get(i);
            String key = new String(Base64.getDecoder().decode(state.key));
            String value;
            Long type = state.value.type;
            if(type == 1) {     // byte string
                if(isAddress(key)) {
                    Address add = new Address(Encoder.decodeFromBase64(state.value.bytes));
                    value = add.toString();
                }
                else {
                    value = new String(Encoder.decodeFromBase64(state.value.bytes));
                }
            }
            else {      // integer
                value = String.valueOf(state.value.uint);
            }
            globalState.put(key, value);
        }
        return globalState;
    }

    /**
     * Check if the given StateSchema key is an Address
     *
     * @param key
     * @return true if the given StateSchema key is an Address, false otherwise
     */
    public static boolean isAddress(String key) {
        return key.equals(GlobalState.Creator.getValue()) || key.equals(GlobalState.EscrowAddress.getValue());
    }


}
