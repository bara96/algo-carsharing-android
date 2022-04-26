package com.example.algorandcarsharing.fragments.trips.created;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.algorand.algosdk.v2.client.model.Application;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.databinding.FragmentTripsCreatedBinding;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.TripModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TripsFragment extends AccountBasedFragment {

    private FragmentTripsCreatedBinding binding;

    private View rootView;

    protected TripAdapter tripAdapter;
    List<TripModel> applications = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TripsViewModel userTripsViewModel =
                new ViewModelProvider(this).get(TripsViewModel.class);

        binding = FragmentTripsCreatedBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        RecyclerView tripList = binding.tripList;
        tripAdapter = new TripAdapter(applications);
        tripList.setAdapter(tripAdapter);
        tripList.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.swipe.setOnRefreshListener(
                () -> {
                    if(account.getAddress() != null) {
                        try {
                            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                                    .thenAcceptAsync(result -> {
                                        List<TripModel> apps = searchApplications(result.createdApps);

                                        // remove old elements
                                        int size = tripAdapter.getItemCount();
                                        applications.clear();
                                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                                        // add new elements
                                        applications.addAll(apps);
                                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeInserted(0, applications.size()));

                                        LogHelper.log("getCreatedApplications", result.toString());
                                        Snackbar.make(rootView, "Refreshed", Snackbar.LENGTH_LONG).show();
                                    })
                                    .exceptionally(e->{
                                        account.setAccountInfo(null);
                                        LogHelper.error("getCreatedApplications", e);
                                        Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
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
                });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<TripModel> searchApplications(List<Application> applications) {
        List<TripModel> validApplications = new ArrayList<>();
        for(int i=0; i<applications.size(); i++) {
            try {
                Application app = applications.get(i);
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
        return validApplications;
    }
}