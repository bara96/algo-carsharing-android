package com.example.algorandcarsharing.fragments.account;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.algorandcarsharing.databinding.FragmentAccountBinding;
import com.example.algorandcarsharing.services.AccountService;
import com.example.algorandcarsharing.services.ApplicationService;
import com.example.algorandcarsharing.models.AccountModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.CompletableFuture;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountModel account;

    private AccountService accountService;
    private View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        accountService = new AccountService();
        account = new AccountModel();

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        rootView = binding.getRoot();

        binding.address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(account.getAddress() != null) {
                    binding.addressLayout.setVisibility(View.VISIBLE);
                }
                else {
                    binding.addressLayout.setVisibility(View.GONE);
                }
            }
        });
        binding.saveBt.setOnClickListener(v -> {
            try {
                String mnemonic = String.valueOf(binding.mnemonic.getText());
                account.setMnemonic(mnemonic);
                binding.address.setText(account.getAddress());
                Snackbar.make(rootView, "Account Saved", Snackbar.LENGTH_LONG).show();
            }
            catch (Exception e) {
                Log.e("Error saving account", e.getMessage());
                Snackbar.make(rootView, String.format("Error while saving the account: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            }
            saveAccountData();
        });

        binding.swipe.setOnRefreshListener(
                () -> {
                    if(account.getAddress() != null) {
                        try {
                            CompletableFuture.supplyAsync(accountService.getAccountInfo(account.getAddress()))
                                    .thenAcceptAsync(result -> {
                                        account.setAccountInfo(result);
                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, "Account Refreshed", Snackbar.LENGTH_LONG).show();
                                    })
                                    .exceptionally(e->{
                                        account.setAccountInfo(null);
                                        binding.swipe.setRefreshing(false);
                                        Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                                        return null;
                                    })
                                    .handle( (ok, ex) -> {
                                        requireActivity().runOnUiThread(() -> binding.balance.setText(String.valueOf(account.getBalance())));
                                        return ok;
                                    });
                        }
                        catch (Exception e) {
                            binding.swipe.setRefreshing(false);
                            Log.e("Error getAccountInfo()", e.getMessage());
                            Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                        }
                    }
                    else {
                        binding.swipe.setRefreshing(false);
                        Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
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
        try {
            account.loadFromStorage(getActivity());
        }
        catch (Exception e) {
            Snackbar.make(rootView, String.format("Error loading account: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        binding.mnemonic.setText(account.getMnemonic());
        binding.address.setText(account.getAddress());
        binding.balance.setText(String.valueOf(account.getBalance()));
        if(account.getMnemonic() == null) {
            Snackbar.make(rootView, "Please set an account address", Snackbar.LENGTH_LONG).show();
        }
    }
}