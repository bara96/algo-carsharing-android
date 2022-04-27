package com.example.algorandcarsharing.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.v2.client.model.ApplicationLocalState;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.example.algorandcarsharing.services.AccountService;
import com.example.algorandcarsharing.services.ApplicationService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountModel {

    protected Account account;
    protected String mnemonic;
    protected Long balance;
    protected com.algorand.algosdk.v2.client.model.Account accountInfo;
    protected ApplicationService applicationService;

    public AccountModel() {
        this.mnemonic = null;
        this.balance = 0L;
        this.accountInfo = null;
        this.account = null;
        this.applicationService = new ApplicationService();
    }

    public AccountModel(String mnemonic) throws Exception {
        this.balance = 0L;
        this.accountInfo = null;
        this.account = null;
        this.applicationService = new ApplicationService();
        this.setMnemonic(mnemonic);
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) throws Exception {
        this.mnemonic = mnemonic;
        try {
            this.account = new Account(this.mnemonic);
        }
        catch (Exception e) {
            this.balance = 0L;
            this.accountInfo = null;
            this.account = null;
            throw new Exception("Invalid mnemonic");
        }
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Account getAccount() {
        return account;
    }

    public void refreshAccountInfo() {
        if(this.getAddress() == null) {
            return;
        }
        try {
            AccountService accountService = new AccountService();
            CompletableFuture.supplyAsync(accountService.getAccountInfo(this.getAddress()))
                    .thenAcceptAsync(result -> {
                        this.setAccountInfo(result);
                        LogHelper.log("refreshAccountInfo()", "Refreshed Account Info");
                    })
                    .exceptionally(e->{
                        LogHelper.error("refreshAccountInfo()", e);
                        return null;
                    });
        }
        catch (Exception e) {
            LogHelper.error("refreshAccountInfo()", e);
        }
    }

    public com.algorand.algosdk.v2.client.model.Account getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(com.algorand.algosdk.v2.client.model.Account accountInfo) {
        this.accountInfo = accountInfo;
        if(accountInfo != null) {
            this.setBalance(accountInfo.amount);
        }
        else {
            this.setBalance(0L);
        }
    }

    public String getAddress() {
        if(this.account != null) {
            return this.account.getAddress().toString();
        }
        return null;
    }

    public ApplicationLocalState getAppLocalState(Long appId) {
        if(this.accountInfo == null) {
            return null;
        }
        List<ApplicationLocalState> appsLocalState = this.accountInfo.appsLocalState;
        for (int i = 0; i < appsLocalState.size(); i++) {
            // search if user has local state for this app
            ApplicationLocalState localStateRaw = appsLocalState.get(i);
            if (localStateRaw.id.equals(appId)) {
                return localStateRaw;
            }
        }

        return null;
    }

    public void loadFromStorage(Context context) throws Exception {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(SharedPreferencesConstants.AccountPreferences.getPreference(), Context.MODE_PRIVATE);
            String mnemonic = sharedPref.getString(SharedPreferencesConstants.AccountPreferences.Mnemonic.getKey(), null);
            if(mnemonic != null) {
                this.setMnemonic(mnemonic);
                this.balance = sharedPref.getLong(SharedPreferencesConstants.AccountPreferences.Balance.getKey(), 0L);
            }
        }
    }

    public void saveToStorage(Context context) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(SharedPreferencesConstants.AccountPreferences.getPreference(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(SharedPreferencesConstants.AccountPreferences.Mnemonic.getKey(), String.valueOf(this.mnemonic).trim());
            System.out.println("saveToStorage");
            System.out.println(this.balance);
            editor.putLong(SharedPreferencesConstants.AccountPreferences.Balance.getKey(), this.balance);
            editor.apply();
        }
    }
}
