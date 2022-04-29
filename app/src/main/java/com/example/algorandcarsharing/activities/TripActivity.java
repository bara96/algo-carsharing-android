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

    private enum TripActionMode {
        Create,
        Join,
        Leave,
        Update,
        Delete,
        End,
    }

    private enum TripViewMode {
        Create,
        Join,
        Leave,
        Update,
        Locked,
        Started,
        Finished,
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

        binding.closeCard.setOnClickListener(view -> binding.cardLayout.setVisibility(View.GONE));

        binding.deleteBt.setOnClickListener(view -> deleteTrip(application));
        binding.sendBt.setOnClickListener(v -> {
            try {
                InsertTripModel tripData = null;
                switch (currentMode) {
                    case Create:
                        tripData = InsertTripModel.validate(rootView);
                        if(tripData != null) {
                            createTrip(tripData);
                        }
                        break;
                    case Update:
                        tripData = InsertTripModel.validate(rootView);
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
                    case Started:
                        endTrip(application);
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
                                application.setLocalState(this.account.getAppLocalState(trip.id()));
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
        if(tripData == null) {
            Snackbar.make(rootView, "Empty trip data", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.Create)) {
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
        if(tripData == null) {
            Snackbar.make(rootView, "Empty trip data", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.Update)) {
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

    private void deleteTrip(TripModel trip) {
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.Delete)) {
            return;
        }

        try {
            setLoading(true);
            CompletableFuture.supplyAsync(applicationService.deleteApplication(trip, account.getAccount()))
                    .thenAcceptAsync(result -> runOnUiThread(() -> {
                        Snackbar.make(rootView, String.format("Deleted trip with id: %s", appId), Snackbar.LENGTH_LONG).show();
                        this.finish();
                    }))
                    .exceptionally(e -> {
                        LogHelper.error("DeleteTrip", e);
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
            LogHelper.error("DeleteTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void endTrip(TripModel trip) {
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.End)) {
            return;
        }

        try {
            setLoading(true);
            CompletableFuture.supplyAsync(applicationService.startTrip(trip, account.getAccount()))
                    .thenAcceptAsync(result -> {
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Started trip with id: %s", appId), Snackbar.LENGTH_LONG).show());
                    })
                    .exceptionally(e -> {
                        LogHelper.error("StartTrip", e);
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
            LogHelper.error("StartTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void joinTrip(TripModel trip) {
        if(trip == null || trip.isEmpty()) {
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.Join)) {
            return;
        }

        try {
            setLoading(true);
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> this.account.setAccountInfo(result))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.participate(trip, account)))
                    .thenAcceptAsync(result -> runOnUiThread(() -> Snackbar.make(rootView, String.format("Joined trip with id: %s", result), Snackbar.LENGTH_LONG).show()))
                    .exceptionally(e -> {
                        LogHelper.error("JoinTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
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
            LogHelper.error("JoinTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void leaveTrip(TripModel trip) {
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(!this.canPerformAction(TripActionMode.Leave)) {
            return;
        }

        try {
            setLoading(true);
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> this.account.setAccountInfo(result))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.cancelParticipation(trip, account)))
                    .thenAcceptAsync(result -> runOnUiThread(() -> {
                        Snackbar.make(rootView, String.format("Left trip with id: %s", result), Snackbar.LENGTH_LONG).show();
                    }))
                    .exceptionally(e -> {
                        LogHelper.error("LeaveTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
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
            LogHelper.error("LeaveTrip", e);
            Snackbar.make(rootView, String.format("Error performing the request: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean canPerformAction(TripActionMode mode) {
        Long minBalance = TransactionsHelper.minFees;

        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return false;
        }

        if(mode == TripActionMode.Create) {
            minBalance += TransactionsHelper.escrowMinBalance;
        }
        else {
            if(appId == null || application == null) {
                Snackbar.make(rootView, "Invalid app id", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }

        if(mode == TripActionMode.Update || mode == TripActionMode.Delete || mode == TripActionMode.End) {
            if(!account.isCreator(appId)) {
                Snackbar.make(rootView, "You are not the creator", Snackbar.LENGTH_LONG).show();
                return false;
            }

            switch (mode) {
                case Update:
                    if(!application.canUpdate()) {
                        Snackbar.make(rootView, "You cannot update the application now", Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    break;
                case Delete:
                    if(!application.canDelete()) {
                        Snackbar.make(rootView, "You cannot delete the application now", Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    break;
                case End:
                    if(!application.canEnd()) {
                        Snackbar.make(rootView, "Trip cannot end now", Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                    break;
            }
        }
        else if(mode == TripActionMode.Join) {
            minBalance += application.cost();
            if(application.isParticipating()) {
                Snackbar.make(rootView, "You are already participating the trip", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }
        else if(mode == TripActionMode.Leave) {
            if(!application.isParticipating()) {
                Snackbar.make(rootView, "You are not participating the trip yet", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }

        if(account.getBalance() < minBalance) {
            Snackbar.make(rootView, String.format("You need at least %s MicroAlgo to perform the request", minBalance), Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
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

        if(trip.isEnded()) {
            return TripViewMode.Finished;
        }

        if(trip.creator().toString().equals(account.getAddress())) {
            // the account is the creator
            if(trip.canEnd()) {
                // trip is about to start
                return TripViewMode.Started;
            }

            if(trip.canUpdate()) {
                // no one is participating, set editable
                return TripViewMode.Update;
            }
            else {
                // at least one participant, set lock mode
                Snackbar.make(rootView, "Cannot edit trip when at least a participant is joined in", Snackbar.LENGTH_LONG).show();
                return TripViewMode.Locked;
            }
        }
        else {
            if(trip.canEnd()) {
                // trip is about to start
                return TripViewMode.Locked;
            }
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
        LogHelper.log("ViewMode", viewMode.toString());

        // delete action hidden by default
        binding.deleteBt.setEnabled(false);
        binding.deleteBt.setVisibility(View.GONE);

        // create dummy action hidden by default
        binding.saveDummyBt.setEnabled(false);
        binding.saveDummyBt.setVisibility(View.GONE);

        // set action buttons
        switch (viewMode) {
            case Join:
                binding.sendBt.setText(getString(R.string.join));
                binding.sendBt.setEnabled(true);
                binding.sendBt.setVisibility(View.VISIBLE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                editEnabled = false;
                break;
            case Leave:
                binding.sendBt.setText(getString(R.string.leave));
                binding.sendBt.setEnabled(true);
                binding.sendBt.setVisibility(View.VISIBLE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                editEnabled = false;
                break;
            case Create:
                binding.sendBt.setText(getString(R.string.create));
                binding.sendBt.setEnabled(true);
                binding.sendBt.setVisibility(View.VISIBLE);
                binding.saveDummyBt.setEnabled(true);
                binding.saveDummyBt.setVisibility(View.VISIBLE);
                binding.cardLayout.setVisibility(View.GONE);
                editEnabled = true;
                break;
            case Update:
                binding.sendBt.setText(getString(R.string.update));
                binding.sendBt.setEnabled(true);
                binding.sendBt.setVisibility(View.VISIBLE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                if(account != null && account.isCreator(appId)) {
                    binding.deleteBt.setEnabled(true);
                    binding.deleteBt.setVisibility(View.VISIBLE);
                }
                editEnabled = true;
                break;
            case Locked:
                binding.sendBt.setText(getString(R.string.update));
                binding.sendBt.setEnabled(false);
                binding.sendBt.setVisibility(View.GONE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                editEnabled = false;
                break;
            case Started:
                binding.sendBt.setText(getString(R.string.end_trip));
                binding.sendBt.setEnabled(true);
                binding.sendBt.setVisibility(View.VISIBLE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                break;
            case Finished:
                binding.sendBt.setEnabled(false);
                binding.sendBt.setVisibility(View.GONE);
                binding.cardLayout.setVisibility(View.VISIBLE);
                if(account != null && account.isCreator(appId)) {
                    binding.deleteBt.setEnabled(true);
                    binding.deleteBt.setVisibility(View.VISIBLE);
                }
                editEnabled = false;
                break;
        }

        if(application != null && !application.isEmpty()) {
            // set status card info
            int maxSeats = Integer.parseInt(application.getGlobalStateKey(ApplicationTripSchema.GlobalState.MaxParticipants));
            int availableSeats = Integer.parseInt(application.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats));
            int participants = maxSeats - availableSeats;
            binding.participants.setText(String.format("%s / %s Participants", participants, maxSeats));

            switch (application.getStatus()) {
                case Finished:
                    binding.status.setText(getString(R.string.status_finished));
                    binding.status.setTextColor(getColor(R.color.blue));
                    break;
                case Started:
                    binding.status.setText(getString(R.string.status_started));
                    binding.status.setTextColor(getColor(R.color.blue));
                    break;
                case Available:
                    if(application.isParticipating()) {
                        binding.status.setText(getString(R.string.status_joined));
                        binding.status.setTextColor(getColor(R.color.yellow));
                    }
                    else {
                        binding.status.setText(getString(R.string.status_available));
                        binding.status.setTextColor(getColor(R.color.green));
                    }
                    break;
                case Full:
                    if(application.isParticipating()) {
                        binding.status.setText(getString(R.string.status_joined));
                        binding.status.setTextColor(getColor(R.color.yellow));
                    }
                    else {
                        binding.status.setText(getString(R.string.status_full));
                        binding.status.setTextColor(getColor(R.color.red));
                    }
                    break;
                default:
                    binding.status.setText(getString(R.string.status_unknown));
                    break;
            }
        }

        // set trip info
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
            binding.deleteBt.setEnabled(false);
            binding.saveDummyBt.setEnabled(false);
        }
        else {
            setTripViewMode(currentMode);
            binding.progressBar.setVisibility(View.GONE);
        }

    }
}