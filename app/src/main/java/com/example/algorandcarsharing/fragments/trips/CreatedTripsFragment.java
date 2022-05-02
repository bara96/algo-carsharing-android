package com.example.algorandcarsharing.fragments.trips;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.algorand.algosdk.v2.client.model.Application;
import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.comparator.TripComparator;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.GenericApplication;
import com.example.algorandcarsharing.models.TripModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreatedTripsFragment extends TripsBasedFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding.label.setText(requireActivity().getString(R.string.created_trips_label));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected void performSearch() {
        if(account.getAddress() == null) {
            binding.swipe.setRefreshing(false);
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                    .thenAcceptAsync(result -> {
                        tripAdapter.setAccount(account);
                        List<TripModel> apps = searchApplications(result.createdApps);

                        // remove old elements
                        int size = tripAdapter.getItemCount();
                        applications.clear();
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                        // add new elements
                        applications.addAll(apps);
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeInserted(0, applications.size()));

                        //LogHelper.log("getCreatedApplications", result.toString());
                        if(apps.size() <= 0) {
                            requireActivity().runOnUiThread(() -> Snackbar.make(rootView, "No application found", Snackbar.LENGTH_LONG).show());
                        }
                    })
                    .exceptionally(e->{
                        account.setAccountInfo(null);
                        LogHelper.error("getCreatedApplications", e);
                        requireActivity().runOnUiThread(() -> Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show());
                        return null;
                    })
                    .handle( (ok, ex) -> {
                        binding.swipe.setRefreshing(false);
                        return ok;
                    });
        }
        catch (Exception e) {
            binding.swipe.setRefreshing(false);
            LogHelper.error("getCreatedApplications", e);
            Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    protected <T> List<TripModel> searchApplications(List<T> applications) {
        List<TripModel> validApplications = new ArrayList<>();
        for(int i=0; i<applications.size(); i++) {
            try {
                Application app = GenericApplication.application(applications.get(i));
                TripModel trip = new TripModel(app);
                if (trip.isValid()) {
                    validApplications.add(trip);
                } else {
                    LogHelper.log("searchApplications", String.format("Application %s is not a trusted application", app.id), LogHelper.LogType.WARNING);
                }
            }
            catch (Exception e) {
                    LogHelper.error("searchApplications", e);
                }
        }
        validApplications.sort(new TripComparator());
        return validApplications;
    }
}