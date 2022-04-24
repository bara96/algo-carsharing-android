package com.example.algorandcarsharing;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.algorandcarsharing.databinding.ActivityTripCreateBinding;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.AccountModel;
import com.example.algorandcarsharing.models.CreateTripModel;
import com.example.algorandcarsharing.pickers.DateSetter;
import com.example.algorandcarsharing.pickers.TimeSetter;
import com.example.algorandcarsharing.services.ApplicationService;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class TripCreateActivity extends AppCompatActivity {

    private ActivityTripCreateBinding binding;
    private AccountModel account;
    private View rootView;

    private ApplicationService applicationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTripCreateBinding.inflate(getLayoutInflater());
        rootView = binding.getRoot();
        setContentView(rootView);
        binding.progressBar.setIndeterminate(true);

        applicationService = new ApplicationService();
        account = new AccountModel();

        new DateSetter(binding.startDate);
        new TimeSetter(binding.startTime);
        new DateSetter(binding.endDate);
        new TimeSetter(binding.endTime);

        binding.saveDummyBt.setOnClickListener(v -> {
            try {
                CreateTripModel tripData = CreateTripModel.DummyTrip();
                saveTrip(tripData);
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
                    saveTrip(tripData);
                }
            }
            catch (Exception e) {
                LogHelper.error("Error saving trip", e);
                Snackbar.make(rootView, String.format("Error while creating the trip: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        loadAccountData();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadAccountData();
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

    private void loadAccountData() {
        try {
            account.loadFromStorage(this);
        }
        catch (Exception e) {
            Snackbar.make(rootView, String.format("Error loading account: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void saveTrip(CreateTripModel tripData) {
        binding.progressBar.setVisibility(View.VISIBLE);
        if(tripData != null && account.getAddress() != null) {
            try {
                CompletableFuture.supplyAsync(applicationService.createApplication(this, account.getAccount(), tripData))
                        .thenAcceptAsync(result -> {
                            LogHelper.log("createApplication()", result.toString());
                            Snackbar.make(rootView, String.format("Trip created with id: %s", result), Snackbar.LENGTH_LONG).show();
                        })
                        .exceptionally(e->{
                            LogHelper.error("createApplication()", e);
                            account.setAccountInfo(null);
                            Snackbar.make(rootView, String.format("Error during creation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                            return null;
                        })
                        .handle( (ok, ex) -> {
                            binding.progressBar.setVisibility(View.GONE);
                            return ok;
                        });
            }
            catch (Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                LogHelper.error("createApplication()", e);
                Snackbar.make(rootView, String.format("Error during creation: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
        }
        else {
            binding.progressBar.setVisibility(View.GONE);
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
}