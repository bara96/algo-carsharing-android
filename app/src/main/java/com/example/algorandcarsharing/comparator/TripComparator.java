package com.example.algorandcarsharing.comparator;

import com.example.algorandcarsharing.models.TripModel;

import java.util.Comparator;

public class TripComparator implements Comparator<TripModel> {
    @Override
    public int compare(TripModel o1, TripModel o2) {
        // order by id desc
        if(o1.id() <= o2.id())
            return 1;
        else
            return 0;
    }
}