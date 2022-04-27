package com.example.algorandcarsharing.activities;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.models.AccountBased;
import com.example.algorandcarsharing.models.AccountModel;
import com.example.algorandcarsharing.services.AccountService;
import com.google.android.material.snackbar.Snackbar;

public abstract class AccountBasedActivity extends AppCompatActivity implements AccountBased {
    protected final AccountService accountService = new AccountService();
    protected AccountModel account = new AccountModel();
    protected View rootView;

    @Override
    public void onResume() {
        super.onResume();
        loadAccountData();
    }

    public void loadAccountData() {
        try {
            account.loadFromStorage(this);

            if(account.getAddress() == null && rootView != null) {
                Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
            }
        }
        catch (Exception e) {
            if(rootView != null) {
                Snackbar.make(rootView, String.format("Error loading account: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
            LogHelper.error(this.getClass().getName(), e, false);
        }
    }
}
