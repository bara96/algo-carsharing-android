package com.example.algorandcarsharing.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.algorand.algosdk.account.Account;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;

public class AccountModel {

    protected Account account;
    protected String mnemonic;
    protected Long balance;
    protected com.algorand.algosdk.v2.client.model.Account accountInfo;

    public AccountModel() {
        this.mnemonic = null;
        this.balance = 0L;
        this.accountInfo = null;
        this.account = null;
    }

    public AccountModel(String mnemonic) throws Exception {
        this.balance = 0L;
        this.accountInfo = null;
        this.account = null;
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

    public void loadFromStorage(Context context) throws Exception {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(SharedPreferencesConstants.AccountPreferences.getPreference(), Context.MODE_PRIVATE);
            String mnemonic = sharedPref.getString(SharedPreferencesConstants.AccountPreferences.Mnemonic.getKey(), null);
            if(mnemonic != null) {
                this.setMnemonic(mnemonic);
                this.balance = sharedPref.getLong(SharedPreferencesConstants.AccountPreferences.Balance.getKey(), 0);
            }
        }
    }

    public void saveToStorage(Context context) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(SharedPreferencesConstants.AccountPreferences.getPreference(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(SharedPreferencesConstants.AccountPreferences.Mnemonic.getKey(), String.valueOf(this.mnemonic).trim());
            editor.putLong(SharedPreferencesConstants.AccountPreferences.Balance.getKey(), this.balance);
            editor.apply();
        }
    }
}
