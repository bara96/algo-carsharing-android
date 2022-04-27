package com.example.algorandcarsharing.activities;

import android.os.Bundle;
import android.view.View;

import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;
import com.example.algorandcarsharing.databinding.ActivityTripBinding;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.InsertTripModel;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.TripSchema;
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
        Update,
        Join,
        Leave,
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
                    InsertTripModel tripData = InsertTripModel.DummyTrip();
                    createTrip(tripData);
                }
            }
            catch (Exception e) {
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }

        });

        binding.sendBt.setOnClickListener(v -> {
            try {
                InsertTripModel tripData = null;
                switch (currentMode) {
                    case Create:
                        tripData = validate();
                        createTrip(tripData);
                        break;
                    case Update:
                        tripData = validate();
                        //createTrip(tripData);
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
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
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
        setLoading(true);
        if(this.account.getAddress() == null) {
            setLoading(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(appId == null) {
            setLoading(false);
            return;
        }
        try {
            setTitle(String.format("Trip %s", appId));
            IndexerService indexerService = new IndexerService();
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> {
                        this.account.setAccountInfo(result);
                    })
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(indexerService.getApplication(appId)))
                    .thenAcceptAsync(result -> {
                        if (!result.application.deleted) {
                            TripModel trip = new TripModel(result.application);
                            if (trip.isValid()) {
                                application = trip;

                                // check if the account is the creator
                                if(trip.creator().toString().equals(account.getAddress())) {
                                    currentMode = TripViewMode.Update;
                                }
                                else {
                                    // check if the account can participate or leave
                                    currentMode = TripViewMode.Join;

                                    ApplicationLocalState localState = this.account.getAppLocalState(trip.id());
                                    if(localState != null) {
                                        // if user has opt-in, check if is participating
                                        trip.readLocalState(localState);
                                        String isParticipating = trip.getLocalStateKey(TripSchema.LocalState.IsParticipating);
                                        if(isParticipating.equals("1")) {
                                            currentMode = TripViewMode.Leave;
                                        }
                                    }
                                }
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
        setLoading(true);
        if(account.getAddress() == null) {
            setLoading(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(tripData == null) {
            setLoading(false);
            Snackbar.make(rootView, "Empty trip data", Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            CompletableFuture.supplyAsync(applicationService.createApplication(getApplicationContext(), account.getAccount(), tripData))
                    .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.initializeEscrow(result, account.getAccount())))
                    .thenAcceptAsync(result -> {
                        runOnUiThread(() ->Snackbar.make(rootView, String.format("Trip created with id: %s", result), Snackbar.LENGTH_LONG).show());
                    })
                    .exceptionally(e -> {
                        LogHelper.error("CreateTrip", e);
                        account.setAccountInfo(null);
                        runOnUiThread(() ->Snackbar.make(rootView, String.format("Error during creation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle((ok, ex) -> {
                        runOnUiThread(() -> setLoading(false));
                        return ok;
                    });
        }
        catch (Exception e) {
            setLoading(false);
            LogHelper.error("CreateTrip", e);
            Snackbar.make(rootView, String.format("Error during creation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void joinTrip(TripModel trip) {
        setLoading(true);
        if(account.getAddress() == null) {
            setLoading(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(trip == null || trip.isEmpty()) {
            setLoading(false);
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            CompletableFuture.supplyAsync(applicationService.participate(trip, account))
                    .thenAcceptAsync(result -> {
                        currentMode = TripViewMode.Leave;
                        runOnUiThread(() -> {
                            setTripViewMode(currentMode);
                            Snackbar.make(rootView, String.format("Joined trip with id: %s", result), Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .exceptionally(e -> {
                        LogHelper.error("JoinTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error during participation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
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
            Snackbar.make(rootView, String.format("Error during participation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    private void leaveTrip(TripModel trip) {
        setLoading(true);
        if(account.getAddress() == null) {
            setLoading(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        if(trip == null || trip.isEmpty()) {
            setLoading(false);
            Snackbar.make(rootView, "Invalid trip", Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            CompletableFuture.supplyAsync(applicationService.cancelParticipation(trip, account))
                    .thenAcceptAsync(result -> {
                        currentMode = TripViewMode.Join;
                        runOnUiThread(() -> {
                            setTripViewMode(currentMode);
                            Snackbar.make(rootView, String.format("Left trip with id: %s", result), Snackbar.LENGTH_LONG).show();
                        });
                    })
                    .exceptionally(e -> {
                        LogHelper.error("LeaveTrip", e);
                        runOnUiThread(() -> Snackbar.make(rootView, String.format("Error during participation cancellation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
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
            Snackbar.make(rootView, String.format("Error during participation cancellation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
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

            if (creatorName.length() < 0) {
                Snackbar.make(rootView, "Creator Name is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (startAddress.length() < 0) {
                Snackbar.make(rootView, "Start Address is required", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (endAddress.length() < 0) {
                Snackbar.make(rootView, "End Address is required", Snackbar.LENGTH_LONG).show();
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

            if(startDatetime.getTime() < endDatetime.getTime()) {
                Snackbar.make(rootView, "End Date must be greater than Start Date", Snackbar.LENGTH_LONG).show();
                return null;
            }

            if (cost < 0) {
                Snackbar.make(rootView, "Cost cannot be 0", Snackbar.LENGTH_LONG).show();
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
            binding.creatorName.setText(trip.getGlobalStateKey(TripSchema.GlobalState.CreatorName));
            binding.startAddress.setText(trip.getGlobalStateKey(TripSchema.GlobalState.DepartureAddress));
            binding.endAddress.setText(trip.getGlobalStateKey(TripSchema.GlobalState.ArrivalAddress));

            binding.cost.setText(trip.getGlobalStateKey(TripSchema.GlobalState.TripCost));
            binding.availableSeats.setText(trip.getGlobalStateKey(TripSchema.GlobalState.AvailableSeats));

            String startDateTime = trip.getGlobalStateKey(TripSchema.GlobalState.DepartureDate);
            String endDateTime = trip.getGlobalStateKey(TripSchema.GlobalState.ArrivalDate);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startDateTime);
            Date endDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(endDateTime);

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

    private void setTripViewMode(TripViewMode viewMode) {
        boolean editEnabled = false;
        switch (viewMode) {
            case Join:
                binding.sendBt.setText(getString(R.string.join));
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = false;
                break;
            case Leave:
                binding.sendBt.setText(getString(R.string.leave));
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = false;
                break;
            case Create:
                binding.sendBt.setText(getString(R.string.create));
                binding.saveDummyBt.setEnabled(true);
                binding.saveDummyBt.setVisibility(View.VISIBLE);
                editEnabled = true;
                break;
            case Update:
                binding.sendBt.setText(getString(R.string.update));
                binding.saveDummyBt.setEnabled(false);
                binding.saveDummyBt.setVisibility(View.GONE);
                editEnabled = true;
                break;
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
        }
        else {
            binding.progressBar.setVisibility(View.GONE);
            binding.sendBt.setEnabled(true);
            binding.saveDummyBt.setEnabled(true);
        }

    }
}