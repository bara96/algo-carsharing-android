package com.example.algorandcarsharing.models;

import com.algorand.algosdk.v2.client.algod.GetStatus;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.example.algorandcarsharing.constants.Constants;
import com.example.algorandcarsharing.helpers.UtilsHelper;

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
import java.util.concurrent.TimeoutException;

public class CreateTripModel implements TripSchema {

    protected String creatorName;
    protected String startAddress;
    protected String endAddress;
    protected Date tripStartDate;
    protected Date tripEndDate;
    protected Integer tripCost;
    protected Integer availableSeats;

    public CreateTripModel(String creatorName, String startAddress, String endAddress, Date tripStartDate, Date tripEndDate, Integer tripCost, Integer availableSeats) {
        this.creatorName = creatorName;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.tripStartDate = tripStartDate;
        this.tripEndDate = tripEndDate;
        this.tripCost = tripCost;
        this.availableSeats = availableSeats;
    }

    public List<byte[]> getArgs(AlgodClient client) throws ExecutionException, InterruptedException {
        List<byte[]> args = new ArrayList<>();

        Long startDateRound = this.datetimeToRounds(client, this.tripStartDate).get();
        Long endDateRounds = this.datetimeToRounds(client, this.tripEndDate).get();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

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
            public Long get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                return this.get();
            }
        };
    }

    public static CreateTripModel DummyTrip() throws ParseException {
        String creatorName = "Matteo Baratella";
        String startAddress = "Mestre";
        String endAddress = "Milano";
        String startDate = "2022/05/10 15:00";
        String endDate = "2022/05/10 21:00";
        Integer cost = Integer.parseInt("5000");
        Integer availableSeats = Integer.parseInt("4");

        Date dateStart = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(startDate);
        Date dateEnd = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(endDate);

        return new CreateTripModel(creatorName, startAddress, endAddress, dateStart, dateEnd, cost, availableSeats);
    }
}
