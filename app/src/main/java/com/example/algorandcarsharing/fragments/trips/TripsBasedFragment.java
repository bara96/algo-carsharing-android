package com.example.algorandcarsharing.fragments.trips;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.algorandcarsharing.adapters.RecyclerLinearLayoutManager;
import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.fragments.account.AccountBasedFragment;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.services.IndexerService;

import java.util.ArrayList;
import java.util.List;

public abstract class TripsBasedFragment extends AccountBasedFragment {

    protected com.example.algorandcarsharing.databinding.FragmentTripListBinding binding;
    protected View rootView;
    protected final IndexerService indexerService = new IndexerService();
    protected TripAdapter tripAdapter;
    protected List<TripModel> applications = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = com.example.algorandcarsharing.databinding.FragmentTripListBinding.inflate(inflater, container, false);
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
    }

    protected abstract void performSearch();

    protected abstract <T> List<TripModel> searchApplications(List<T> applications);
}