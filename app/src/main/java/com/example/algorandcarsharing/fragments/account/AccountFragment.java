package com.example.algorandcarsharing.fragments.account;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.example.algorandcarsharing.databinding.FragmentAccountBinding;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;
import com.example.algorandcarsharing.helpers.LogHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.CompletableFuture;

public class AccountFragment extends AccountBasedFragment {

    private FragmentAccountBinding binding;

    private View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

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
                LogHelper.error("Error saving account", e);
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
                                        LogHelper.log("getAccountInfo()", result.toString());
                                        Snackbar.make(rootView, "Account Refreshed", Snackbar.LENGTH_LONG).show();
                                    })
                                    .exceptionally(e->{
                                        account.setAccountInfo(null);
                                        LogHelper.error("getAccountInfo()", e);
                                        Snackbar.make(rootView, String.format("Error during refresh: %s", e.getMessage()), Snackbar.LENGTH_LONG).show();
                                        return null;
                                    })
                                    .handle( (ok, ex) -> {
                                        requireActivity().runOnUiThread(() -> binding.balance.setText(String.valueOf(account.getBalance())));
                                        binding.swipe.setRefreshing(false);
                                        return ok;
                                    });
                        }
                        catch (Exception e) {
                            binding.swipe.setRefreshing(false);
                            LogHelper.error("getAccountInfo()", e);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void saveAccountData() {
        account.saveToStorage(getActivity());
    }

    @Override
    protected void loadAccountData() {
        super.loadAccountData();

        binding.mnemonic.setText(account.getMnemonic());
        binding.address.setText(account.getAddress());
        binding.balance.setText(String.valueOf(account.getBalance()));
    }
}