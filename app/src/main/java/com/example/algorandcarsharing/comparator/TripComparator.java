package com.example.algorandcarsharing.comparator;

import com.example.algorandcarsharing.models.TripModel;

import java.util.Comparator;

public class TripComparator implements Comparator<TripModel> {
    @Override
    public int compare(TripModel o1, TripModel o2) {
        return o1.id().compareTo(o2.id());
    }
}