package com.example.algorandcarsharing;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.example.algorandcarsharing.helpers.IndexerHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.algorandcarsharing.databinding.ActivityMainBinding;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private IndexerHelper indexerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExecutorService mExecutor = Executors.newSingleThreadExecutor();
        indexerHelper = new IndexerHelper(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mExecutor.execute(new Runnable(){
                    @Override
                    public void run(){
                        JSONObject response = null;
                        try {
                            response = indexerHelper.searchTransactions();
                            Log.d("Indexer Response", response.toString(2));
                        }
                        catch (Exception e){
                            Log.e("Indexer Error", Arrays.toString(e.getStackTrace()));
                        }
                    }
                });
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_account)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences accountPreferences = this.getSharedPreferences(getString(R.string.preferences_account), Context.MODE_PRIVATE);
        final String address = accountPreferences.getString("address", null);
        if(address == null) {
            /*
            NavHostFragment finalHost = NavHostFragment.create(R.navigation.mobile_navigation);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_account, finalHost)
                    .setPrimaryNavigationFragment(finalHost)
                    .commit();
             */
        }
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
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}