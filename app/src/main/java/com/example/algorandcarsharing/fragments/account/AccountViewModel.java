package com.example.algorandcarsharing.fragments.account;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AccountViewModel extends ViewModel {

    private final MutableLiveData<String> balance;
    private final MutableLiveData<String> address;

    public AccountViewModel() {
        balance = new MutableLiveData<>();
        address = new MutableLiveData<>();
    }

    public LiveData<String> getBalance() {
        return balance;
    }

    public LiveData<String> getAddress() {
        return address;
    }
}