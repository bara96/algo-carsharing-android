package com.example.algorandcarsharing.fragments;

import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.algorandcarsharing.models.AccountModel;
import com.example.algorandcarsharing.services.AccountService;
import com.google.android.material.snackbar.Snackbar;

public abstract class AccountBasedFragment extends Fragment {
    protected final AccountService accountService = new AccountService();
    protected AccountModel account = new AccountModel();
    protected View rootView;

    @Override
    public void onStart() {
        super.onStart();

        loadAccountData();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadAccountData();
    }

    protected void loadAccountData() {
        try {
            account.loadFromStorage(getActivity());
        }
        catch (Exception e) {
            Snackbar.make(rootView, String.format("Error loading account: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        if(account.getMnemonic() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
        }
    }
}
