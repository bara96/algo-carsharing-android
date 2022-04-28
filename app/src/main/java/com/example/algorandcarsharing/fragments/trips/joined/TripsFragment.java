package com.example.algorandcarsharing.fragments.trips.joined;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.example.algorandcarsharing.adapters.RecyclerLinearLayoutManager;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.databinding.FragmentTripsJoinedBinding;
import com.example.algorandcarsharing.fragments.trips.TripsBasedFragment;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.GenericApplication;
import com.example.algorandcarsharing.models.TripModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TripsFragment extends TripsBasedFragment {

    private FragmentTripsJoinedBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TripsViewModel userTripsViewModel =
                new ViewModelProvider(this).get(TripsViewModel.class);

        binding = FragmentTripsJoinedBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        RecyclerView tripList = binding.tripList;
        tripAdapter = new TripAdapter(applications);
        tripList.setAdapter(tripAdapter);

        LinearLayoutManager layoutManager= new RecyclerLinearLayoutManager(getActivity());
        tripList.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(tripList.getContext(), layoutManager.getOrientation());
        tripList.addItemDecoration(dividerItemDecoration);

        binding.swipe.setOnRefreshListener(this::performSearch);

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
                        List<TripModel> apps = searchApplications(result.appsLocalState);

                        // remove old elements
                        int size = tripAdapter.getItemCount();
                        applications.clear();
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                        // add new elements
                        applications.addAll(apps);
                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeInserted(0, applications.size()));

                        //LogHelper.log("getJoinedApplications", result.toString());
                        if(apps.size() <= 0) {
                            requireActivity().runOnUiThread(() -> Snackbar.make(rootView, "No application found", Snackbar.LENGTH_LONG).show());
                        }
                    })
                    .exceptionally(e->{
                        account.setAccountInfo(null);
                        LogHelper.error("getJoinedApplications", e);
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
            LogHelper.error("getJoinedApplications", e);
            Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
        }
    }

    protected <T> List<TripModel> searchApplications(List<T> applications) {
        List<CompletableFuture> futureList=new ArrayList<>();
        List<TripModel> validApplications = new ArrayList<>();
        for(int i=0; i<applications.size(); i++) {
            try {
                ApplicationLocalState appLocalState = GenericApplication.applicationLocalState(applications.get(i));
                futureList.add(CompletableFuture.supplyAsync(indexerService.getApplication(appLocalState.id))
                        .thenAcceptAsync(result -> {
                            if(!result.application.deleted) {
                                TripModel trip = new TripModel(result.application);
                                if(trip.isValid()) {
                                    trip.setLocalState(appLocalState);
                                    if(trip.isParticipating()) {
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
        return validApplications;
    }
}