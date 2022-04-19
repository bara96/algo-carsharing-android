package com.example.algorandcarsharing.fragments.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.databinding.FragmentAccountBinding;
import com.example.algorandcarsharing.helpers.ApplicationHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private long balance = 0;
    private String address = null;

    private ApplicationHelper applicationHelper;
    private View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        Context context = getActivity();
        ExecutorService mExecutor = Executors.newSingleThreadExecutor();

        applicationHelper = new ApplicationHelper(context);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        binding.saveBt.setOnClickListener(v -> {
            address = String.valueOf(binding.addressEt.getText());
            saveAccountData();
            Snackbar.make(rootView, "Address saved", Snackbar.LENGTH_SHORT).show();
        });

        binding.swipe.setOnRefreshListener(
                () -> {
                    if(address != null) {
                        mExecutor.execute(() -> {
                            try {
                                balance = applicationHelper.getBalance(address);
                                saveAccountData();
                                requireActivity().runOnUiThread(() -> binding.balanceTv.setText(String.valueOf(balance)));
                            }
                            catch (Exception e){
                                Log.e("Request Error", e.getMessage());
                                Snackbar.make(rootView, String.format("Error refreshing balance: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();

                            }
                            finally {
                                binding.swipe.setRefreshing(false);
                            }
                        });
                    }
                });

        // accountViewModel.getBalance().observe(getViewLifecycleOwner(), balanceTextView::setText);
        // accountViewModel.getAddress().observe(getViewLifecycleOwner(), addressEditText::setText);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        loadAccountData();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadAccountData();
        if(this.address == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void saveAccountData() {
        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preferences_account), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(getString(R.string.preference_key_address), String.valueOf(address).trim());
            editor.putLong(getString(R.string.preference_key_balance), balance);
            editor.apply();
        }
    }

    private void loadAccountData() {
        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preferences_account), Context.MODE_PRIVATE);
            this.address = sharedPref.getString(getString(R.string.preference_key_address), null);
            this.balance = sharedPref.getLong(getString(R.string.preference_key_balance), 0);

            binding.addressEt.setText(this.address);
            binding.balanceTv.setText(String.valueOf(this.balance));

        }
    }
}