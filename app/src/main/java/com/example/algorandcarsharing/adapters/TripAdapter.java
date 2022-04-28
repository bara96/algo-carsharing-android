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
import com.example.algorandcarsharing.models.AccountModel;
import com.example.algorandcarsharing.models.TripModel;
import com.example.algorandcarsharing.models.ApplicationTripSchema;

import java.util.List;
import java.util.Objects;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    protected List<TripModel> localDataSet;
    protected AccountModel account = null;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title, status, cost, availability, startAddress, startDate, endAddress, endDate;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            title = view.findViewById(R.id.title);
            status = view.findViewById(R.id.status);
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

    /**
     * Set an Account
     *
     * @param account
     */
    public void setAccount(AccountModel account) {
        this.account = account;
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

        Context context = viewHolder.itemView.getContext();
        TripModel trip = localDataSet.get(position);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        String id = String.valueOf(trip.id());
        String cost = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.TripCost);
        String availability = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.AvailableSeats);
        String maxParticipants = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.MaxParticipants);
        String startAddress = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureAddress);
        String startDate = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureDate);
        String endAddress = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.ArrivalAddress);
        String endDate = trip.getGlobalStateKey(ApplicationTripSchema.GlobalState.ArrivalDate);

        viewHolder.title.setText(String.format("Trip n° %s", id));
        viewHolder.availability.setText(String.format("%s / %s Seats", availability, maxParticipants));
        viewHolder.startAddress.setText(startAddress);
        viewHolder.startDate.setText(startDate);
        viewHolder.endAddress.setText(endAddress);
        viewHolder.endDate.setText(endDate);
        viewHolder.cost.setText(cost);

        switch (trip.getStatus()) {
            case Finished:
                viewHolder.status.setText(context.getString(R.string.status_finished));
                viewHolder.status.setTextColor(context.getColor(R.color.blue));
                break;
            case Starting:
                viewHolder.status.setText(context.getString(R.string.status_starting));
                viewHolder.status.setTextColor(context.getColor(R.color.blue));
                break;
            case Available:
                if(trip.isParticipating()) {
                    viewHolder.status.setText(context.getString(R.string.status_joined));
                    viewHolder.status.setTextColor(context.getColor(R.color.yellow));
                }
                else {
                    viewHolder.status.setText(context.getString(R.string.status_available));
                    viewHolder.status.setTextColor(context.getColor(R.color.green));
                }
                break;
            case Full:
                viewHolder.status.setText(context.getString(R.string.status_full));
                viewHolder.status.setTextColor(context.getColor(R.color.red));
                break;
            default:
                viewHolder.status.setText(context.getString(R.string.status_unknown));
                break;
        }

        viewHolder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, TripActivity.class);
            intent.putExtra(SharedPreferencesConstants.IntentExtra.AppId.getKey(), trip.id());
            context.startActivity(intent);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}
