package com.example.algorandcarsharing.fragments.trips;

import android.view.View;

import com.example.algorandcarsharing.adapters.TripAdapter;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.services.IndexerService;

import java.util.ArrayList;
import java.util.List;

public abstract class TripsBasedFragment extends AccountBasedFragment {

    protected View rootView;
    protected final IndexerService indexerService = new IndexerService();
    protected TripAdapter tripAdapter;
    protected List<TripModel> applications = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        performSearch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    protected abstract void performSearch();

    protected abstract <T> List<TripModel> searchApplications(List<T> applications);
}