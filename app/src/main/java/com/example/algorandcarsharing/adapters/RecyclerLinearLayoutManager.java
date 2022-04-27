package com.example.algorandcarsharing.adapters;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.algorandcarsharing.helpers.LogHelper;

public class RecyclerLinearLayoutManager extends LinearLayoutManager {
    public RecyclerLinearLayoutManager(Context context) {
        super(context);
    }

    public RecyclerLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public RecyclerLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //Generate constructors

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        try {
            super.onLayoutChildren(recycler, state);

        } catch (IndexOutOfBoundsException e) {
            LogHelper.error(this.getClass().getName(), e, false);
        }

    }
}