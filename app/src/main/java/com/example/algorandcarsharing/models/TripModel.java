package com.example.algorandcarsharing.models;

import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.algorand.algosdk.v2.client.model.TealKeyValue;
import com.example.algorandcarsharing.constants.ApplicationConstants;
import com.example.algorandcarsharing.helpers.LogHelper;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class TripModel implements ApplicationTripSchema {

    protected Application application;
    protected HashMap<String, String> globalState = new HashMap<>();
    protected HashMap<String, String> localState = new HashMap<>();

    // trip states
    public enum TripStatus {
        Available,
        Full,
        Started,
        Finished,
    }

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
            return false;
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
     * Check if the application trip is started
     *
     * @return true if the trip is started, false otherwise
     */
    public boolean isEnded(){
        // if the application is in "Started" mode it means that the escrow is closed: trip is ended
        return Integer.parseInt(this.getGlobalStateKey(GlobalState.TripState)) == (ApplicationState.Finished.getValue());
    }

    /**
     * Check if the application trip can start
     *
     * @return true if the trip can start, false otherwise
     */
    public boolean canEnd() {
        if(this.isEmpty()) {
            return false;
        }
        if(this.isEnded()) {
            // trip is already ended
            return false;
        }

        try {
            Date now = new Date();
            String departureDateTime = this.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureDate);
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
     * Check if the application trip be deleted.
     *
     * @return true if the trip can be deleted., false otherwise
     */
    public boolean canDelete() {
        if(this.isEmpty()) {
            return false;
        }
        if(this.isEnded()) {
            // trip is already started
            return true;
        }

        return this.canUpdate();
    }

    /**
     * Check if the trip can be updated.
     * A trip can be edited if no one is participating and the trip cannot start
     *
     * @return true if the Trip can be edited, false otherwise
     */
    public boolean canUpdate() {
        int maxSeats = Integer.parseInt(this.getGlobalStateKey(ApplicationTripSchema.GlobalState.MaxParticipants));
        int availableSeats = Integer.parseInt(this.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats));

        return !this.canEnd() && maxSeats == availableSeats;
    }

    /**
     * Check if the user is participating.
     * Requires the LocalState to be set
     *
     * @return true if the LocalState is participating, false otherwise
     */
    public boolean isParticipating() {
        String isParticipating = this.getLocalStateKey(LocalState.IsParticipating);
        return Objects.equals(isParticipating, "1");
    }

    public TripStatus getStatus() {
        if(Integer.parseInt(this.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripState)) == (ApplicationTripSchema.ApplicationState.Finished.getValue())) {
            // trip is already started, no more editable
            return TripStatus.Finished;
        }
        if(this.canEnd()) {
            return TripStatus.Started;
        }
        if(Integer.parseInt(this.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats)) == 0) {
            return TripStatus.Full;
        }
        return TripStatus.Available;
    }

    public String getGlobalStateKey(GlobalState key) {
        if(this.globalState == null) {
            return null;
        }
        return this.globalState.getOrDefault(key.getValue(), null);
    }

    public String getLocalStateKey(LocalState key) {
        if(this.localState == null) {
            return null;
        }
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
        else {
            this.localState = null;
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
    protected static boolean isAddress(String key) {
        return key.equals(GlobalState.Creator.getValue()) || key.equals(GlobalState.EscrowAddress.getValue());
    }
}
