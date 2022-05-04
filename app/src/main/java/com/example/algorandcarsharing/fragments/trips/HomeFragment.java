package com.example.algorandcarsharing.fragments.trips;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.algorand.algosdk.v2.client.model.Transaction;
import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.comparator.TripComparator;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.GenericApplication;
import com.example.algorandcarsharing.models.TripModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HomeFragment extends TripsBasedFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding.label.setText(requireActivity().getString(R.string.available_trips_label));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected void performSearch() {
        try {
            CompletableFuture.supplyAsync(indexerService.getTransactions())
                    .thenAcceptAsync(result -> {
                        tripAdapter.setAccount(account);
                        List<TripModel> apps = searchApplications(result.transactions);

                        // remove old elements
                        int size = tripAdapter.getItemCount();
                        applications.clear();
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                        // add new elements
                        applications.addAll(apps);
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeInserted(0, applications.size()));

                        //LogHelper.log("getAllApplications", result.toString());
                        if(apps.size() <= 0) {
                            requireActivity().runOnUiThread(() -> Snackbar.make(rootView, "No application found", Snackbar.LENGTH_LONG).show());
                        }
                    })
                    .exceptionally(e->{
                        // remove old elements
                        int size = tripAdapter.getItemCount();
                        applications.clear();
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                        LogHelper.error("getAllApplications", e);
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
            LogHelper.error("getAllApplications", e);
            Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    protected <T> List<TripModel> searchApplications(List<T> transactions) {
        List<CompletableFuture> futureList=new ArrayList<>();
        List<TripModel> validApplications = new ArrayList<>();
        for(int i=0; i<transactions.size(); i++) {
            try {
                Transaction transaction = GenericApplication.transaction(transactions.get(i));
                futureList.add(CompletableFuture.supplyAsync(indexerService.getApplication(transaction.createdApplicationIndex))
                        .thenAcceptAsync(result -> {
                            if(!result.application.deleted) {
                                TripModel trip = new TripModel(result.application);
                                if(trip.isValid()) {
                                    if(account != null) {
                                        // if account is set, remove user created trips
                                        trip.setLocalState(account.getAppLocalState(trip.id()));
                                        if(account.getCreatedApps(trip.id()) == null) {
                                            validApplications.add(trip);
                                        }
                                    }
                                    else {
                                        // add all trips
                                        validApplications.add(trip);
                                    }
                                }
                                else {
                                    LogHelper.log("getApplication()", String.format("Application %s is not a trusted application", result.application.id), LogHelper.LogType.WARNING);
                                }
                            }
                        })
                        .exceptionally(e->{
                            LogHelper.error("getApplication()", e, false);
                            return null;
                        }));
            }
            catch (Exception e) {
                LogHelper.error("getApplication()", e, false);
            }
        }
        futureList.forEach(CompletableFuture::join);
        validApplications.sort(new TripComparator().reversed());
        return validApplications;
    }

}