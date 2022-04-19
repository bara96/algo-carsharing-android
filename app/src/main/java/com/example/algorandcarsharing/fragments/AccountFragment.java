package com.example.algorandcarsharing.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.databinding.FragmentAccountBinding;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private float balance = 0;
    private String address = null;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();
        if (context != null) {
            SharedPreferences accountPreferences = context.getSharedPreferences(getString(R.string.preferences_account), Context.MODE_PRIVATE);
            this.address = accountPreferences.getString("address", null);
        }

        if(this.address == null) {

        }

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView balanceTextView = binding.balanceTv;
        EditText addressEditText = binding.addressEt;

        binding.swipe.setOnRefreshListener(
                () -> {
                    balance++;
                    balanceTextView.setText(String.valueOf(balance));
                    binding.swipe.setRefreshing(false);
                });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}