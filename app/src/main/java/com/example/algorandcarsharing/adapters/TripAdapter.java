package com.example.algorandcarsharing.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.activities.TripActivity;
import com.example.algorandcarsharing.constants.SharedPreferencesConstants;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.TripSchema;

import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    protected List<TripModel> localDataSet;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title, cost, availability, startAddress, startDate, endAddress, endDate;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            title = view.findViewById(R.id.title);
            cost = view.findViewById(R.id.cost);
            availability = view.findViewById(R.id.availability);
            startAddress = view.findViewById(R.id.start_address);
            startDate = view.findViewById(R.id.start_date);
            endAddress = view.findViewById(R.id.end_address);
            endDate = view.findViewById(R.id.end_date);
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView.
     */
    public TripAdapter(List<TripModel> dataSet) {
        localDataSet = dataSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.trip_row_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        String id = String.valueOf(localDataSet.get(position).id());
        String cost = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.TripCost);
        String availability = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.AvailableSeats);
        String startAddress = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.DepartureAddress);
        String startDate = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.DepartureDate);
        String endAddress = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.ArrivalAddress);
        String endDate = localDataSet.get(position).getGlobalStateKey(TripSchema.GlobalState.ArrivalDate);

        viewHolder.title.setText(String.format("Trip n° %s", id));
        viewHolder.cost.setText(cost);
        viewHolder.availability.setText(availability);
        viewHolder.startAddress.setText(startAddress);
        viewHolder.startDate.setText(startDate);
        viewHolder.endAddress.setText(endAddress);
        viewHolder.endDate.setText(endDate);

        viewHolder.itemView.setOnClickListener(view -> {
            Context context = view.getContext();
            Intent intent = new Intent(context, TripActivity.class);
            intent.putExtra(SharedPreferencesConstants.IntentExtra.AppId.getKey(), localDataSet.get(position).id());
            context.startActivity(intent);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}
