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
import com.algorand.algosdk.v2.client.model.Transaction;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.databinding.FragmentTripsCreatedBinding;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.helpers.ServicesHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TripsFragment extends AccountBasedFragment {

    private FragmentTripsCreatedBinding binding;

    private View rootView;

    protected TripAdapter tripAdapter;
    List<Application> applications = new ArrayList<>();

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
                                        List<Application> apps = searchApplications(result.createdApps);

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

    private List<Application> searchApplications(List<Application> applications) {
        List<Application> validApplications = new ArrayList<>();
        for(int i=0; i<applications.size(); i++) {
            Application app = applications.get(i);
            if(ServicesHelper.isTrustedApplication(app)) {
                validApplications.add(app);
            }
            else {
                LogHelper.log("searchApplications", String.format("Application %s is not a trusted application", app.id), LogHelper.LogType.WARNING);
            }
        }
        return validApplications;
    }
}