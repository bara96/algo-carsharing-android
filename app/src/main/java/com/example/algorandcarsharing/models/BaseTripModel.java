package com.example.algorandcarsharing.models;


import com.algorand.algosdk.logic.StateSchema;
import com.algorand.algosdk.v2.client.model.Application;

public interface BaseTripModel {

    // global state fields
    public static StateSchema getGlobalStateSchema() {
        return new StateSchema(7, 5);
    }

    public enum GlobalSTate {
        Creator("creator"),
        CreatorName("creator_name"),
        DepartureAddress("departure_address"),
        ArrivalAddress("arrival_address"),
        DepartureDate("departure_date"),
        ArrivalDate("arrival_date"),
        MaxParticipants("max_participants"),
        TripCost("trip_cost"),
        TripState("trip_state"),
        AvailableSeats("available_seats"),
        EscrowAddress("escrow_address");

        private final String value;

        GlobalSTate(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // local state fields
    public static StateSchema getLocalStateSchema() {
        return new StateSchema(1, 0);
    }

    public enum LocalState {
        IsParticipating("is_participating");

        private final String value;

        LocalState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // available app methods
    public enum AppMethod {
        InitializeEscrow("initializeEscrow"),
        UpdateTrip("updateTrip"),
        CancelTrip("cancelTrip"),
        StartTrip("startTrip"),
        Participate("participateTrip"),
        CancelParticipation("cancelParticipation");

        private final String value;

        AppMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // trip states
    public enum TripState {
        NotInitialized(0),
        Initialized(1),
        Started(2);

        private final Integer value;

        TripState(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    // user states
    public enum UserState {
        Participating(1),
        NotParticipating(0);

        private final Integer value;

        UserState(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }


}
