package com.example.algorandcarsharing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.algorandcarsharing.databinding.ActivityMainBinding;
import com.example.algorandcarsharing.services.IndexerService;
import com.google.android.material.navigation.NavigationView;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private IndexerService indexerService;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Security.removeProvider("BC");
        Security.insertProviderAt(new BouncyCastleProvider(), 0);

        ExecutorService mExecutor = Executors.newSingleThreadExecutor();
        indexerService = new IndexerService(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, TripCreate.class);
            startActivity(intent);
            /*
            mExecutor.execute(() -> {
                JSONObject response = null;
                try {
                    response = indexerService.searchTransactions();
                    Log.d("Indexer Response", response.toString(2));
                    Snackbar.make(view, "Indexer searchTransactions", Snackbar.LENGTH_LONG).show();
                }
                catch (Exception e){
                    Log.e("Indexer Error", e.getMessage());
                }
            });
             */
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_trips_created, R.id.nav_trips_joined, R.id.nav_account)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        checkAccount();
        if(item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void checkAccount() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_account), Context.MODE_PRIVATE);
        final String mnemonic = sharedPref.getString(getString(R.string.preference_key_mnemonic), null);
        if(mnemonic == null) {
            navController.navigate(R.id.nav_account);
        }
    }
}