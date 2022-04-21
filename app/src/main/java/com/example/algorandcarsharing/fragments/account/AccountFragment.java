package com.example.algorandcarsharing.fragments.account;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.algorandcarsharing.databinding.FragmentAccountBinding;
import com.example.algorandcarsharing.services.ApplicationService;
import com.example.algorandcarsharing.models.AccountModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountModel account;

    private ApplicationService applicationService;
    private View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        Context context = getActivity();
        ExecutorService mExecutor = Executors.newSingleThreadExecutor();

        applicationService = new ApplicationService(context);
        account = new AccountModel();

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        binding.saveBt.setOnClickListener(v -> {
            account.setAddress(String.valueOf(binding.addressEt.getText()));
            saveAccountData();
            Snackbar.make(rootView, "Address saved", Snackbar.LENGTH_SHORT).show();
        });

        binding.swipe.setOnRefreshListener(
                () -> {
                    if(account.getAddress() != null) {
                        try {
                            CompletableFuture.supplyAsync(applicationService.getBalance(account.getAddress()))
                                    .thenAcceptAsync(result -> {
                                        account.setBalance(result);
                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, "Account Refreshed", Snackbar.LENGTH_LONG).show();
                                    })
                                    .exceptionally(e->{
                                        account.setBalance(0L);
                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, String.format("Error refreshing balance: %s", e.getMessage()), Snackbar.LENGTH_SHORT).show();
                                        return null;
                                    })
                                    .handle( (ok, ex) -> {
                                        requireActivity().runOnUiThread(() -> binding.balanceTv.setText(String.valueOf(account.getBalance())));
                                        return ok;
                                    });
                        }
                        catch (Exception e) {
                            binding.swipe.setRefreshing(false);
                            Log.e("Request Error", e.getMessage());
                            Snackbar.make(rootView, String.format("Error refreshing balance: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });

        // accountViewModel.getBalance().observe(getViewLifecycleOwner(), balanceTextView::setText);
        // accountViewModel.getAddress().observe(getViewLifecycleOwner(), addressEditText::setText);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        saveAccountData();
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
        if(account.getAddress() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void saveAccountData() {
        account.saveToStorage(getActivity());
    }

    private void loadAccountData() {
        account.loadFromStorage(getActivity());
        binding.addressEt.setText(account.getAddress());
        binding.balanceTv.setText(String.valueOf(account.getBalance()));
    }
}