package com.example.algorandcarsharing.activities;

import android.os.Bundle;
import android.view.View;

import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;
import com.example.algorandcarsharing.databinding.ActivityTripBinding;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.CreateTripModel;
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

    private ApplicationService applicationService;

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
                CreateTripModel tripData = CreateTripModel.DummyTrip();
                createTrip(tripData);
            }
            catch (Exception e) {
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }

        });

        binding.saveBt.setOnClickListener(v -> {
            try {
                CreateTripModel tripData = validate();
                if(tripData != null) {
                    createTrip(tripData);
                }
            }
            catch (Exception e) {
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
        });

        Long appId = null;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                appId = extras.getLong(SharedPreferencesConstants.IntentExtra.AppId.getKey());
                loadApplication(appId);
            }
        } else {
            appId = (Long) savedInstanceState.getSerializable(SharedPreferencesConstants.IntentExtra.AppId.getKey());
            loadApplication(appId);
        }
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
        if(appId != null) {
            try {
                IndexerService indexerService = new IndexerService();
                CompletableFuture.supplyAsync(indexerService.getApplication(appId))
                        .thenAcceptAsync(result -> {
                            if (!result.application.deleted) {
                                TripModel trip = new TripModel(result.application);
                                if (trip.isValid()) {
                                    application = trip;
                                    runOnUiThread(() -> {
                                        setTripOnView(application);
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
    }

    private void createTrip(CreateTripModel tripData) {
        setLoading(true);
        if(tripData != null && account.getAddress() != null) {
            try {
                CompletableFuture.supplyAsync(applicationService.createApplication(getApplicationContext(), account.getAccount(), tripData))
                        .thenComposeAsync(result -> CompletableFuture.supplyAsync(applicationService.initializeEscrow(result, account.getAccount())))
                        .thenAcceptAsync(result -> {
                            Snackbar.make(rootView, String.format("Trip created with id: %s", result), Snackbar.LENGTH_LONG).show();
                        })
                        .exceptionally(e -> {
                            LogHelper.error("CreateTrip", e);
                            account.setAccountInfo(null);
                            Snackbar.make(rootView, String.format("Error during creation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
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
        else {
            setLoading(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
        }
    }

    private CreateTripModel validate() throws ParseException {
            String creatorName = String.valueOf(binding.creatorName.getText()).trim();
            String startAddress = String.valueOf(binding.startAddress.getText()).trim();
            String endAddress = String.valueOf(binding.endAddress.getText()).trim();
            String startDate = String.valueOf(binding.startDate.getText()).trim();
            String startTime = String.valueOf(binding.startTime.getText()).trim();
            String endDate = String.valueOf(binding.endDate.getText()).trim();
            String endTime = String.valueOf(binding.endTime.getText()).trim();

            int cost = Integer.parseInt(String.valueOf(binding.cost.getText()));
            int availableSeats = Integer.parseInt(String.valueOf(binding.availableSeats.getText()));

            Date startDatetime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(startDate + " " + startTime);
            Date endDatetime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(endDate + " " + endTime);

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

            return new CreateTripModel(creatorName, startAddress, endAddress, startDatetime, endDatetime, cost, availableSeats);
    }

    private void setTripOnView(TripModel trip) {
        try {
            String startDateTime = trip.getGlobalStateKey(TripSchema.GlobalState.DepartureDate);
            String endDateTime = trip.getGlobalStateKey(TripSchema.GlobalState.ArrivalDate);
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            DateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date startDatetime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(startDateTime);
            Date endDatetime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(endDateTime);

            binding.saveBt.setText(getString(R.string.update));
            binding.creatorName.setText(trip.getGlobalStateKey(TripSchema.GlobalState.CreatorName));
            binding.startAddress.setText(trip.getGlobalStateKey(TripSchema.GlobalState.DepartureAddress));
            binding.endAddress.setText(trip.getGlobalStateKey(TripSchema.GlobalState.ArrivalAddress));

            binding.startDate.setText(dateFormat.format(startDatetime));
            binding.startTime.setText(timeFormat.format(startDatetime));

            binding.endDate.setText(dateFormat.format(endDatetime));
            binding.endTime.setText(timeFormat.format(endDatetime));

            binding.cost.setText(trip.getGlobalStateKey(TripSchema.GlobalState.TripCost));
            binding.availableSeats.setText(trip.getGlobalStateKey(TripSchema.GlobalState.AvailableSeats));
        }
        catch (Exception e) {
            Snackbar.make(rootView, "Error loading trip", Snackbar.LENGTH_LONG).show();
            LogHelper.error("setShowView", e);
            application = null;
        }
    }

    private void setTripViewEditing(boolean enabled) {
        binding.creatorName.setEnabled(enabled);
        binding.startAddress.setEnabled(enabled);
        binding.endAddress.setEnabled(enabled);
        binding.startDate.setEnabled(enabled);
        binding.startTime.setEnabled(enabled);
        binding.endDate.setEnabled(enabled);
        binding.endTime.setEnabled(enabled);
        binding.cost.setEnabled(enabled);
        binding.availableSeats.setEnabled(enabled);
    }

    private void setLoading(boolean isLoading) {
        if(isLoading) {
            this.setTripViewEditing(false);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.saveBt.setEnabled(false);
            binding.saveDummyBt.setEnabled(false);
        }
        else {
            this.setTripViewEditing(true);
            binding.progressBar.setVisibility(View.GONE);
            binding.saveBt.setEnabled(true);
            binding.saveDummyBt.setEnabled(true);
        }

    }
}