package com.example.algorandcarsharing.models;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.algorand.algosdk.v2.client.model.TealKeyValue;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.constants.Constants;
import com.example.algorandcarsharing.helpers.LogHelper;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TripModel implements TripSchema {

    protected Application application;
    protected HashMap<String, String> globalState = new HashMap<>();
    protected HashMap<String, String> localState = new HashMap<>();

    public TripModel(Application application) {
        this.application = application;
        setGlobalState(application);
    }

    /**
     *
     * @return the Application id
     */
    public Long id() {
        return this.application.id;
    }

    /**
     * @return the Application cost
     */
    public Long cost() {
        return Long.valueOf(this.getGlobalStateKey(GlobalState.TripCost));
    }

    /**
     *
     * @return the Application creator Address
     */
    public Address creator() {
        return this.application.params.creator;
    }

    /**
     *
     * @return the Application creator Address
     */
    public Address escrowAddress() throws NoSuchAlgorithmException {
        return new Address(this.getGlobalStateKey(GlobalState.EscrowAddress));
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

    /**
     * Check if the application trip can start
     *
     * @return true if the trip can start, false otherwise
     */
    public boolean canStart() {
        if(this.isEmpty()) {
            return false;
        }
        try {
            Date now = new Date();
            String departureDateTime = this.getGlobalStateKey(TripSchema.GlobalState.DepartureDate);
            Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(departureDateTime);
            if(startDatetime != null && startDatetime.getTime() - now.getTime() < 0) {
                return true;
            }
        }
        catch (Exception e) {
            LogHelper.error(this.getClass().getName(), e);
        }
        return false;
    }

    /**
     * Check if the user is participating.
     * Requires the LocalState to be set
     *
     * @return true if the LocalState is participating, false otherwise
     */
    public boolean isParticipating() {
        String isParticipating = this.getLocalStateKey(LocalState.IsParticipating);
        return isParticipating.equals("1");
    }

    public String getGlobalStateKey(GlobalState key) {
        return this.globalState.getOrDefault(key.getValue(), null);
    }

    public String getLocalStateKey(LocalState key) {
        return this.localState.getOrDefault(key.getValue(), null);
    }

    public HashMap<String, String> getGlobalState() {
        return globalState;
    }

    /**
     * Read the LocalState StateSchema of the application
     *
     * @param application
     */
    public void setLocalState(ApplicationLocalState application) {
        if(application != null) {
            this.localState = readState(application.keyValue);
        }
    }

    /**
     * Read the GlobalState StateSchema of the application
     *
     * @param application
     */
    public void setGlobalState(Application application) {
        if(application.params != null && application.params.globalState != null) {
            this.globalState = readState(application.params.globalState);
        }
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
