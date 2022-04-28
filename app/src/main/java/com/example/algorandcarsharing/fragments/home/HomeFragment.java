package com.example.algorandcarsharing.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.algorand.algosdk.v2.client.model.Transaction;
import com.example.algorandcarsharing.adapters.RecyclerLinearLayoutManager;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.databinding.FragmentHomeBinding;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.services.IndexerService;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HomeFragment extends AccountBasedFragment {

    private FragmentHomeBinding binding;
    private final IndexerService indexerService = new IndexerService();
    private View rootView;
    protected TripAdapter tripAdapter;
    protected List<TripModel> applications = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
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
    public void onResume() {
        super.onResume();
        account.refreshAccountInfo();
        performSearch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void performSearch() {
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

    private List<TripModel> searchApplications(List<Transaction> transactions) {
        List<CompletableFuture> futureList=new ArrayList<>();
        List<TripModel> validApplications = new ArrayList<>();
        for(int i=0; i<transactions.size(); i++) {
            try {
                Transaction transaction = transactions.get(i);
                futureList.add(CompletableFuture.supplyAsync(indexerService.getApplication(transaction.createdApplicationIndex))
                        .thenAcceptAsync(result -> {
                            if(!result.application.deleted) {
                                TripModel trip = new TripModel(result.application);
                                if(trip.isValid()) {
                                    trip.setLocalState(account.getAppLocalState(trip.id()));
                                    validApplications.add(trip);
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