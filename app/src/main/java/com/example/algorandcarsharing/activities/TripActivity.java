package com.example.algorandcarsharing.activities;

import android.os.Bundle;
import android.view.View;

import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;
import com.example.algorandcarsharing.databinding.ActivityTripBinding;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.models.InsertTripModel;
import com.example.algorandcarsharing.models.TripFactory;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.ApplicationTripSchema;
import com.example.algorandcarsharing.pickers.DateSetter;
import com.example.algorandcarsharing.pickers.TimeSetter;
import com.example.algorandcarsharing.services.ApplicationService;
import com.example.algorandcarsharing.services.IndexerService;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class TripActivity extends AccountBasedActivity {

    private TripModel application = null;
    private ActivityTripBinding binding;
    private View rootView;
    private Long appId = null;
    private TripViewMode currentMode = TripViewMode.Create;

    private ApplicationService applicationService;

    private enum TripViewMode {
        Create,
        Join,
        Leave,
        Update,
        Locked,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTripBinding.inflate(getLayoutInflater());
        rootView = binding.getRoot();
        setContentView(rootView);
        binding.progressBar.setIndeterminate(true);

        applicationService = new ApplicationService();

        new DateSetter(binding.startDate);
        new TimeSetter(binding.startTime);
        new DateSetter(binding.endDate);
        new TimeSetter(binding.endTime);

        binding.saveDummyBt.setOnClickListener(v -> {
            try {
                if(currentMode == TripViewMode.Create) {
                    InsertTripModel tripData = TripFactory.getTrip();
                    createTrip(tripData);
                }
            }
            catch (Exception e) {
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }

        });

        binding.startBt.setOnClickListener(view -> {
            if(application != null && application.canStart()) {
                startTrip(application);
            }
        });

        binding.sendBt.setOnClickListener(v -> {
            try {
                InsertTripModel tripData = null;
                switch (currentMode) {
                    case Create:
                        tripData = validate();
                        if(tripData != null) {
                            createTrip(tripData);
                        }
                        break;
                    case Update:
                        tripData = validate();
                        if(tripData != null) {
                            updateTrip(tripData);
                        }
                        break;
                    case Join:
                        joinTrip(application);
                        break;
                    case Leave:
                        leaveTrip(application);
                        break;
                }
            }
            catch (Exception e) {
                LogHelper.error("Error validating trip", e);
                Snackbar.make(rootView, String.format("Error while validating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
        });

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                appId = extras.getLong(SharedPreferencesConstants.IntentExtra.AppId.getKey());
            }
        } else {
            appId = (Long) savedInstanceState.getSerializable(SharedPreferencesConstants.IntentExtra.AppId.getKey());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setTripViewMode(currentMode);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadApplication(appId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void loadApplication(Long appId) {
        if(this.account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(appId == null) {
            return;
        }
        try {
            setLoading(true);
            setTitle(String.format("Trip %s", appId));
            IndexerService indexerService = new IndexerService();
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> this.account.setAccountInfo(result))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(indexerService.getApplication(appId)))
                    .thenAcceptAsync(result -> {
                        if (!result.application.deleted) {
                            TripModel trip = new TripModel(result.application);
                            if (trip.isValid()) {
                                application = trip;
                                currentMode = getTripViewMode(trip);

                                runOnUiThread(() -> {
                                    setTripOnView(application);
                                    setTripViewMode(currentMode);
                                });
                            } else {
                                LogHelper.log("loadApplication()", String.format("Application %s is not a trusted application", result.application.id), LogHelper.LogType.WARNING);
                            }
                        }
                    })
                    .exceptionally(e -> {
                        LogHelper.error("loadApplication()", e, false);
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> setLoading(false));
                        return ok;
                    });
        } catch (Exception e) {
            setLoading(false);
            LogHelper.error("loadApplication()", e, false);
        }
    }

    private void createTrip(InsertTripModel tripData) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(tripData == null) {
            Snackbar.make(rootView, "Empty trip data", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(account.getBalance() < (TransactionsHelper.escrowMinBalance + TransactionsHelper.minFees)) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to create a trip", (TransactionsHelper.escrowMinBalance + TransactionsHelper.minFees)), Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            setLoading(true);
            CompletableFuture.supplyAsync(applicationService.createApplication(getApplicationContext(), account.getAccount(), tripData))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.initializeEscrow(result, account.getAccount())))
                    .thenAcceptAsync(result -> {
                        appId = result;
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Trip created with id: %s", appId), Snackbar.LENGTH_LONG).show());
                    })
                    .exceptionally(e -> {
                        LogHelper.error("CreateTrip", e);
                        appId = null;
                        runOnUiThread(() ->Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> {
                            setLoading(false);
                            if(appId != null) {
                                loadApplication(appId);
                            }
                        });
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("CreateTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void updateTrip(InsertTripModel tripData) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(tripData == null) {
            Snackbar.make(rootView, "Empty trip data", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(appId == null) {
            Snackbar.make(rootView, "Invalid app id", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(account.getBalance() < TransactionsHelper.minFees) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to update the trip", TransactionsHelper.minFees), Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            setLoading(true);
            CompletableFuture.supplyAsync(applicationService.updateApplication(appId, account.getAccount(), tripData))
                    .thenAcceptAsync(result -> runOnUiThread(() -> Snackbar.make(rootView, String.format("Updated trip with id: %s", appId), Snackbar.LENGTH_LONG).show()))
                    .exceptionally(e -> {
                        LogHelper.error("UpdateTrip", e);
                        runOnUiThread(() ->Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> {
                            setLoading(false);
                            if(appId != null) {
                                loadApplication(appId);
                            }
                        });
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("UpdateTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void startTrip(TripModel trip) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(account.getBalance() < TransactionsHelper.minFees) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to start the trip", TransactionsHelper.minFees), Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            setLoading(true);
            CompletableFuture.supplyAsync(applicationService.startTrip(trip, account.getAccount()))
                    .thenAcceptAsync(result -> {
                        currentMode = TripViewMode.Locked;
                        runOnUiThread(() -> {
                            setTripViewMode(currentMode);
                            Snackbar.make(rootView, String.format("Started trip with id: %s", appId), Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .exceptionally(e -> {
                        LogHelper.error("StartTrip", e);
                        runOnUiThread(() ->Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> setLoading(false));
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("StartTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void joinTrip(TripModel trip) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(account.getBalance() < (trip.cost() + TransactionsHelper.minFees)) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to join the trip", (trip.cost() + TransactionsHelper.minFees)), Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            setLoading(true);
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> this.account.setAccountInfo(result))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.participate(trip, account)))
                    .thenAcceptAsync(result -> {
                        currentMode = TripViewMode.Leave;
                        runOnUiThread(() -> {
                            setTripViewMode(currentMode);
                            Snackbar.make(rootView, String.format("Joined trip with id: %s", result), Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .exceptionally(e -> {
                        LogHelper.error("JoinTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> setLoading(false));
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("JoinTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void leaveTrip(TripModel trip) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(account.getBalance() < TransactionsHelper.minFees) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to leave the trip", TransactionsHelper.minFees), Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            setLoading(true);
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> this.account.setAccountInfo(result))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.cancelParticipation(trip, account)))
                    .thenAcceptAsync(result -> {
                        currentMode = TripViewMode.Join;
                        runOnUiThread(() -> {
                            setTripViewMode(currentMode);
                            Snackbar.make(rootView, String.format("Left trip with id: %s", result), Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .exceptionally(e -> {
                        LogHelper.error("LeaveTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> setLoading(false));
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("LeaveTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private InsertTripModel validate() throws ParseException {
            String creatorName = String.valueOf(binding.creatorName.getText()).trim();
            String startAddress = String.valueOf(binding.startAddress.getText()).trim();
            String endAddress = String.valueOf(binding.endAddress.getText()).trim();
            String startDate = String.valueOf(binding.startDate.getText()).trim();
            String startTime = String.valueOf(binding.startTime.getText()).trim();
            String endDate = String.valueOf(binding.endDate.getText()).trim();
            String endTime = String.valueOf(binding.endTime.getText()).trim();

            int cost = Integer.parseInt(String.valueOf(binding.cost.getText()));
            int availableSeats = Integer.parseInt(String.valueOf(binding.availableSeats.getText()));

            Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startDate + " " + startTime);
            Date endDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(endDate + " " + endTime);
            Date now = new Date();

            if (creatorName.length() <= 0) {
                Snackbar.make(rootView, "Creator Name is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (startAddress.length() <= 0) {
                Snackbar.make(rootView, "Departure Address is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (endAddress.length() <= 0) {
                Snackbar.make(rootView, "Arrival Address is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (startDatetime == null) {
                Snackbar.make(rootView, "Start Date is invalid", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (endDatetime == null) {
                Snackbar.make(rootView, "End Date is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if(startDatetime.getTime() < now.getTime()) {
                Snackbar.make(rootView, "Start Date must be greater than now", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if(endDatetime.getTime() < startDatetime.getTime()) {
                Snackbar.make(rootView, "End Date must be greater than Start Date", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (cost < 1000) {
                Snackbar.make(rootView, "Cost cannot be less than 1000", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (availableSeats < 0) {
                Snackbar.make(rootView, "Seats cannot be 0", Snackbar.LENGTH_LONG).show();
                return null;
            }

            return new InsertTripModel(creatorName, startAddress, endAddress, startDatetime, endDatetime, cost, availableSeats);
    }

    private void setTripOnView(TripModel trip) {
        try {
            binding.sendBt.setText(getString(R.string.update));
            binding.creatorName.setText(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.CreatorName));
            binding.startAddress.setText(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureAddress));
            binding.endAddress.setText(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.ArrivalAddress));

            binding.cost.setText(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost));
            binding.availableSeats.setText(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats));

            String departureDateTime = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureDate);
            String arrivalDateTime = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.ArrivalDate);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(departureDateTime);
            Date endDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(arrivalDateTime);

            binding.startDate.setText(dateFormat.format(startDatetime));
            binding.startTime.setText(timeFormat.format(startDatetime));

            binding.endDate.setText(dateFormat.format(endDatetime));
            binding.endTime.setText(timeFormat.format(endDatetime));
        }
        catch (Exception e) {
            Snackbar.make(rootView, "Error loading trip", Snackbar.LENGTH_LONG).show();
            LogHelper.error("setShowView", e);
            application = null;
        }
    }

    private TripViewMode getTripViewMode(TripModel trip) {
        int availableSeats = Integer.parseInt(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats));

        if(Integer.parseInt(trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripState)) == ApplicationTripSchema.ApplicationState.Started.getValue()) {
            // the trip is already started, set the lock mode
            return TripViewMode.Locked;
        }

        if(trip.creator().toString().equals(account.getAddress())) {
            // the account is the creator
            if(trip.isEditable()) {
                // no one is participating, set editable
                return TripViewMode.Update;
            }
            else {
                // at least one participant, set lock mode
                return TripViewMode.Locked;
            }
        }
        else {
            // the account is a participant, can either participate or leave
            if(availableSeats < 0) {
                // no available seats
                return TripViewMode.Locked;
            }

            ApplicationLocalState localState = this.account.getAppLocalState(trip.id());
            if(localState != null) {
                // if user has opt-in, check if is participating
                trip.setLocalState(localState);
                if(trip.isParticipating()) {
                    return TripViewMode.Leave;
                }
            }
           return TripViewMode.Join;
        }
    }

    private void setTripViewMode(TripViewMode viewMode) {
        boolean editEnabled = false;
        switch (viewMode) {
            case Join:
                binding.sendBt.setText(getString(R.string.join));
                binding.sendBt.setEnabled(true);
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = false;
                break;
            case Leave:
                binding.sendBt.setText(getString(R.string.leave));
                binding.sendBt.setEnabled(true);
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = false;
                break;
            case Create:
                binding.sendBt.setText(getString(R.string.create));
                binding.sendBt.setEnabled(true);
                binding.saveDummyBt.setEnabled(true);
                binding.saveDummyBt.setVisibility(View.VISIBLE);
                editEnabled = true;
                break;
            case Update:
                binding.sendBt.setText(getString(R.string.update));
                binding.sendBt.setEnabled(true);
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = true;
                break;
            case Locked:
                binding.sendBt.setText(getString(R.string.update));
                binding.sendBt.setEnabled(false);
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = false;
                break;
        }

        if(application != null && application.canStart()) {
            binding.startBt.setEnabled(true);
            binding.startBt.setVisibility(View.VISIBLE);
        }
        else {
            binding.startBt.setEnabled(false);
            binding.startBt.setVisibility(View.GONE);
        }

        binding.creatorName.setEnabled(editEnabled);
        binding.startAddress.setEnabled(editEnabled);
        binding.endAddress.setEnabled(editEnabled);
        binding.startDate.setEnabled(editEnabled);
        binding.startTime.setEnabled(editEnabled);
        binding.endDate.setEnabled(editEnabled);
        binding.endTime.setEnabled(editEnabled);
        binding.cost.setEnabled(editEnabled);
        binding.availableSeats.setEnabled(editEnabled);
    }

    private void setLoading(boolean isLoading) {
        if(isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.sendBt.setEnabled(false);
            binding.saveDummyBt.setEnabled(false);
            binding.startBt.setEnabled(false);
        }
        else {
            setTripViewMode(currentMode);
            binding.progressBar.setVisibility(View.GONE);
        }

    }
}