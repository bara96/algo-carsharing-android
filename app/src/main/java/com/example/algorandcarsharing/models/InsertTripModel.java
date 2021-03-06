package com.example.algorandcarsharing.models;

import android.view.View;
import android.widget.EditText;

import com.algorand.algosdk.v2.client.algod.GetStatus;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.example.algorandcarsharing.R;
import com.example.algorandcarsharing.constants.Constants;
import com.example.algorandcarsharing.helpers.UtilsHelper;
import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class InsertTripModel implements ApplicationTripSchema {

    protected String creatorName;
    protected String startAddress;
    protected String endAddress;
    protected Date tripStartDate;
    protected Date tripEndDate;
    protected Integer tripCost;
    protected Integer availableSeats;

    public InsertTripModel(String creatorName, String startAddress, String endAddress, Date tripStartDate, Date tripEndDate, Integer tripCost, Integer availableSeats) {
        this.creatorName = creatorName;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.tripStartDate = tripStartDate;
        this.tripEndDate = tripEndDate;
        this.tripCost = tripCost;
        this.availableSeats = availableSeats;
    }

    public InsertTripModel(TripModel tripModel) throws ParseException {
        String departureDateTime = tripModel.getGlobalStateKey(ApplicationTripSchema.GlobalState.DepartureDate);
        String arrivalDateTime = tripModel.getGlobalStateKey(ApplicationTripSchema.GlobalState.ArrivalDate);
        Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(departureDateTime);
        Date endDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(arrivalDateTime);

        this.creatorName = tripModel.getGlobalStateKey(GlobalState.CreatorName);
        this.startAddress = tripModel.getGlobalStateKey(GlobalState.DepartureAddress);
        this.endAddress = tripModel.getGlobalStateKey(GlobalState.ArrivalAddress);
        this.tripStartDate = startDatetime;
        this.tripEndDate = endDatetime;
        this.tripCost = Integer.valueOf(tripModel.getGlobalStateKey(GlobalState.TripCost));
        this.availableSeats = Integer.valueOf(tripModel.getGlobalStateKey(GlobalState.AvailableSeats));
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public Date getTripStartDate() {
        return tripStartDate;
    }

    public void setTripStartDate(Date tripStartDate) {
        this.tripStartDate = tripStartDate;
    }

    public Date getTripEndDate() {
        return tripEndDate;
    }

    public void setTripEndDate(Date tripEndDate) {
        this.tripEndDate = tripEndDate;
    }

    public Integer getTripCost() {
        return tripCost;
    }

    public void setTripCost(Integer tripCost) {
        this.tripCost = tripCost;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public List<byte[]> getArgs(AlgodClient client) throws ExecutionException, InterruptedException {
        List<byte[]> args = new ArrayList<>();

        Long startDateRound = this.datetimeToRounds(client, this.tripStartDate).get();
        Long endDateRounds = this.datetimeToRounds(client, this.tripEndDate).get();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        args.add(this.creatorName.getBytes(StandardCharsets.UTF_8));
        args.add(this.startAddress.getBytes(StandardCharsets.UTF_8));
        args.add(this.endAddress.getBytes(StandardCharsets.UTF_8));
        args.add(dateFormat.format(this.tripStartDate).getBytes(StandardCharsets.UTF_8));
        args.add(UtilsHelper.IntToBytes(Math.toIntExact(startDateRound)));
        args.add(dateFormat.format(this.tripEndDate).getBytes(StandardCharsets.UTF_8));
        args.add(UtilsHelper.IntToBytes(Math.toIntExact(endDateRounds)));
        args.add(UtilsHelper.IntToBytes(this.tripCost));
        args.add(UtilsHelper.IntToBytes(this.availableSeats));

        return args;
    }

    public static InsertTripModel validate(View view) throws ParseException {
        EditText creatorNameView = view.findViewById(R.id.creator_name);
        EditText startAddressView = view.findViewById(R.id.start_address);
        EditText endAddressView = view.findViewById(R.id.end_address);
        EditText startDateView = view.findViewById(R.id.start_date);
        EditText startTimeView = view.findViewById(R.id.start_time);
        EditText endDateView = view.findViewById(R.id.end_date);
        EditText endTimeView = view.findViewById(R.id.end_time);
        EditText costView = view.findViewById(R.id.cost);
        EditText availableSeatsView = view.findViewById(R.id.available_seats);

        String creatorName = String.valueOf(creatorNameView.getText());
        String startAddress = String.valueOf(startAddressView.getText());
        String endAddress = String.valueOf(endAddressView.getText());
        String startDate = String.valueOf(startDateView.getText());
        String startTime = String.valueOf(startTimeView.getText());
        String endDate = String.valueOf(endDateView.getText());
        String endTime = String.valueOf(endTimeView.getText());

        if(costView.getText().length() <= 0 || availableSeatsView.getText().length() <= 0) {
            Snackbar.make(view, "Invalid number", Snackbar.LENGTH_LONG).show();
            return null;
        }

        int cost = Integer.parseInt(String.valueOf(costView.getText()));
        int availableSeats = Integer.parseInt(String.valueOf(availableSeatsView.getText()));

        Date startDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startDate + " " + startTime);
        Date endDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(endDate + " " + endTime);
        Date now = new Date();

        if (creatorName.length() <= 0) {
            Snackbar.make(view, "Creator Name is required", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (startAddress.length() <= 0) {
            Snackbar.make(view, "Departure Address is required", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (endAddress.length() <= 0) {
            Snackbar.make(view, "Arrival Address is required", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (startDatetime == null) {
            Snackbar.make(view, "Start Date is invalid", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (endDatetime == null) {
            Snackbar.make(view, "End Date is required", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if(startDatetime.getTime() < now.getTime()) {
            Snackbar.make(view, "Start Date must be greater than now", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if(endDatetime.getTime() < startDatetime.getTime()) {
            Snackbar.make(view, "End Date must be greater than Start Date", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (cost < 1000) {
            Snackbar.make(view, "Cost cannot be less than 1000", Snackbar.LENGTH_LONG).show();
            return null;
        }

        if (availableSeats < 0) {
            Snackbar.make(view, "Seats cannot be 0", Snackbar.LENGTH_LONG).show();
            return null;
        }

        return new InsertTripModel(creatorName, startAddress, endAddress, startDatetime, endDatetime, cost, availableSeats);
    }

    public Future<Long> datetimeToRounds(AlgodClient client, Date date) {
        return new Future<Long>() {
            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Long get() throws ExecutionException, InterruptedException {
                try {
                    GetStatus status = client.GetStatus();

                    Date today = new Date();

                    long diffSeconds = (date.getTime()- today.getTime()) / 1000;

                    if(diffSeconds < 0L) {
                        return 0L;
                    }

                    double nBlocksProduced = diffSeconds / Constants.blockSpeed;
                    double round = status.execute().body().lastRound + nBlocksProduced;

                    return Math.round(round);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }

            @Override
            public Long get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException {
                return this.get();
            }
        };
    }
}
