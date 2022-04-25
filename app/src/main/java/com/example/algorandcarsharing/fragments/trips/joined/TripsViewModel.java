package com.example.algorandcarsharing.fragments.trips.joined;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TripsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TripsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is joined trips fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}