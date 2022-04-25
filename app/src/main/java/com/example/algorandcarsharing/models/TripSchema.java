package com.example.algorandcarsharing.models;


import com.algorand.algosdk.logic.StateSchema;

public interface TripSchema {

    // global state fields
    public static StateSchema getGlobalStateSchema() {
        return new StateSchema(7, 7);
    }

    public enum GlobalState {
        Creator("creator"),
        CreatorName("creator_name"),
        DepartureAddress("departure_address"),
        ArrivalAddress("arrival_address"),
        DepartureDate("departure_date"),
        DepartureDateRound("departure_date_round"),
        ArrivalDate("arrival_date"),
        ArrivalDateRound("arrival_date_round"),
        MaxParticipants("max_participants"),
        TripCost("trip_cost"),
        TripState("trip_state"),
        AvailableSeats("available_seats"),
        EscrowAddress("escrow_address");

        private final String value;

        GlobalState(String value) {
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
