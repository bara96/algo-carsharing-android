package com.example.algorandcarsharing.fragments.trips.joined;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.algorandcarsharing.databinding.FragmentTripsJoinedBinding;
import com.example.algorandcarsharing.fragments.AccountBasedFragment;

public class TripsFragment extends AccountBasedFragment {

    private FragmentTripsJoinedBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TripsViewModel userTripsViewModel =
                new ViewModelProvider(this).get(TripsViewModel.class);

        binding = FragmentTripsJoinedBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textGallery;
        userTripsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}