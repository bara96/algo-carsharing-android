package com.example.algorandcarsharing.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class TripFactory implements ApplicationTripSchema {

    public static String getName() {
        List<String> names = new ArrayList<>(Arrays.asList("Neha Santiago", "Alvin Reyna", "Jack Corona", "Karam Adkins", "Humera Downes", "Beatrix Dickinson", "Cerys Bartlett", "Rukhsar Petersen", "Blessing Benton"));

        Random r = new Random();
        int index = r.nextInt(names.size());

        return names.get(index);
    }

    public static String getAddress() {
        List<String> addresses = new ArrayList<>(Arrays.asList("Miami", "Cincinnati", "Brooklyn", "Orlando", "Las Vegas", "Washington", "Houston", "Los Angeles", "New York"));

        Random r = new Random();
        int index = r.nextInt(addresses.size());

        return addresses.get(index);
    }

    public static String getAddress(String exclude) {
        List<String> addresses = new ArrayList<>(Arrays.asList("Miami", "Cincinnati", "Brooklyn", "Orlando", "Las Vegas", "Washington", "Houston", "Los Angeles", "New York"));
        addresses.remove(exclude);

        Random r = new Random();
        int index = r.nextInt(addresses.size());

        return addresses.get(index);
    }

    public static Date getDate(int startFromDays, int endToDays) {
        long now = new Date().getTime();

        // set minimum start days from now
        long startDays = TimeUnit.DAYS.toMillis(startFromDays);
        Date startDate = new Date(now + startDays);
        long startMillis = startDate.getTime();

        // set maximum end days from now
        long endDays = TimeUnit.DAYS.toMillis(endToDays);
        Date endDate = new Date(now + endDays);
        long endMillis = endDate.getTime();

        // random date between startDate and endDate
        long randomMillisSinceEpoch = ThreadLocalRandom
                .current()
                .nextLong(startMillis, endMillis);

        return new Date(randomMillisSinceEpoch);
    }

    public static Integer getAvailability() {
        return getNumber(2, 5);
    }

    public static Integer getCost() {
        return getNumber(1000, 6000);
    }

    public static Integer getNumber(Integer min, Integer max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static InsertTripModel getTrip() {
        String creatorName = getName();
        String startAddress = getAddress();
        String endAddress = getAddress(startAddress);

        int randomDays = getNumber(10, 30);
        Date dateStart = getDate(randomDays, randomDays+1);
        Date dateEnd = getDate(randomDays+1, randomDays+2);
        Integer cost = getCost();
        Integer availableSeats = getAvailability();

        return new InsertTripModel(creatorName, startAddress, endAddress, dateStart, dateEnd, cost, availableSeats);
    }
}
