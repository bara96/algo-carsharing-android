package com.example.algorandcarsharing.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.algorandcarsharing.R;

public class AccountModel {

    protected String address;
    protected Long balance;

    public AccountModel() {
        this.address = null;
        this.balance = 0L;
    }

    public AccountModel(String address, Long balance) {
        this.address = address;
        this.balance = balance;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public void loadFromStorage(Context context) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_account), Context.MODE_PRIVATE);
            this.address = sharedPref.getString(context.getString(R.string.preference_key_address), null);
            this.balance = sharedPref.getLong(context.getString(R.string.preference_key_balance), 0);
        }
    }

    public void saveToStorage(Context context) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_account), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(context.getString(R.string.preference_key_address), String.valueOf(this.address).trim());
            editor.putLong(context.getString(R.string.preference_key_balance), this.balance);
            editor.apply();
        }
    }
}
