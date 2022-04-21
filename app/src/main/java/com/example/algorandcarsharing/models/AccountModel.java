package com.example.algorandcarsharing.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.algorandcarsharing.R;
import com.algorand.algosdk.account.Account;

import java.security.GeneralSecurityException;

public class AccountModel {

    protected Account account;
    protected String mnemonic;
    protected long balance;

    public AccountModel() {
        this.mnemonic = null;
        this.balance = 0L;
        this.account = null;
    }

    public AccountModel(String mnemonic) throws GeneralSecurityException {
        this.mnemonic = mnemonic;
        this.balance = 0L;
        this.account = null;
        loadAccount();
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
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

    public void loadAccount() throws GeneralSecurityException {
        this.account = new Account(this.mnemonic);
    }

    public String getAddress() {
        if(this.account != null) {
            return this.account.getAddress().toString();
        }
        return null;
    }

    public void loadFromStorage(Context context) throws GeneralSecurityException {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_account), Context.MODE_PRIVATE);
            this.mnemonic = sharedPref.getString(context.getString(R.string.preference_key_mnemonic), null);
            this.balance = sharedPref.getLong(context.getString(R.string.preference_key_balance), 0);
            if(this.mnemonic != null) {
                loadAccount();
            }
        }
    }

    public void saveToStorage(Context context) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_account), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(context.getString(R.string.preference_key_mnemonic), String.valueOf(this.mnemonic).trim());
            editor.putLong(context.getString(R.string.preference_key_balance), this.balance);
            editor.apply();
        }
    }
}
