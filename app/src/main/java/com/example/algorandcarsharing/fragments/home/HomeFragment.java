package com.example.algorandcarsharing.fragments.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.algorand.algosdk.v2.client.model.Application;
import com.algorand.algosdk.v2.client.model.ApplicationResponse;
import com.algorand.algosdk.v2.client.model.Transaction;
import com.algorand.algosdk.v2.client.model.TransactionsResponse;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.databinding.FragmentHomeBinding;
import com.example.algorandcarsharing.services.ApplicationService;
import com.example.algorandcarsharing.services.IndexerService;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private IndexerService indexerService;
    private View rootView;

    protected TripAdapter tripAdapter;
    List<Application> applications;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        indexerService = new IndexerService();

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        applications = new ArrayList<>();
        RecyclerView tripList = binding.tripList;
        tripAdapter = new TripAdapter(applications);
        tripList.setAdapter(tripAdapter);
        tripList.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.swipe.setOnRefreshListener(
                () -> {
                        try {
                            CompletableFuture.supplyAsync(indexerService.getTransactions())
                                    .thenAcceptAsync(result -> {
                                        List<Application> apps = searchApplications(result.transactions);
                                        System.out.println(apps);

                                        // remove old elements
                                        int size = tripAdapter.getItemCount();
                                        applications.clear();
                                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                                        // add new elements
                                        applications.addAll(apps);
                                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeInserted(0, applications.size()));

                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, "Refreshed", Snackbar.LENGTH_LONG).show();
                                    })
                                    .exceptionally(e->{
                                        // remove old elements
                                        int size = tripAdapter.getItemCount();
                                        applications.clear();
                                        requireActivity().runOnUiThread(() -> tripAdapter.notifyItemRangeRemoved(0, size));

                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_SHORT).show();
                                        return null;
                                    });
                        }
                        catch (Exception e) {
                            binding.swipe.setRefreshing(false);
                            Log.e("Request Error", e.getMessage());
                            Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                        }
                });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<Application> searchApplications(List<Transaction> transactions) {
        List<CompletableFuture> futureList=new ArrayList<>();
        List<Application> validApplications = new ArrayList<>();
        for(int i=0; i<transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            try {
                futureList.add(CompletableFuture.supplyAsync(indexerService.getApplication(transaction.createdApplicationIndex))
                        .thenAcceptAsync(result -> {
                            if(!result.application.deleted) {
                                validApplications.add(result.application);
                            }
                        }).exceptionally(e->{
                            Log.e("Request Error", e.getMessage());
                        return null;
                    }));
            }
            catch (Exception e) {
                Log.e("Request Error", e.getMessage());
            }
        }
        futureList.forEach(CompletableFuture::join);
        return validApplications;
    }

}