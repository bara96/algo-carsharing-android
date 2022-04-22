package com.example.algorandcarsharing;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.algorand.algosdk.crypto.TEALProgram;
import com.example.algorandcarsharing.helpers.TransactionsHelper;
import com.example.algorandcarsharing.services.ApplicationService;

import java.util.Objects;

public class TripCreate extends AppCompatActivity {

    private ApplicationService applicationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_create);

        applicationService = new ApplicationService();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}