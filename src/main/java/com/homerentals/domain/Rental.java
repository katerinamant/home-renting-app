package com.homerentals.domain;

import org.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class Rental implements Serializable {
    private final int id;
    private final HostAccount hostAccount;
    private final String roomName;
    private final String location;
    private final double nightlyRate;
    private final int capacity;
    private final RatingsAggregator ratings;

    private final HashMap<Integer, CalendarYear> availability;

    private final String imagePath;

    public Rental(
            HostAccount hostAccount,
            String roomName,
            String location,
            double nightlyRate,
            int capacity,
            int numOfReviews,
            int sumOfReviews,
            String imagePath,
            int id
    ) {
        this.hostAccount = hostAccount;
        this.roomName = roomName;
        this.location = location;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
        this.ratings = new RatingsAggregator(numOfReviews, sumOfReviews);
        this.imagePath = imagePath;
        this.id = id;
        this.availability = new HashMap<Integer, CalendarYear>();
    }

    public int getId() {
        return this.id;
    }

    public HostAccount getHostAccount() {
        return this.hostAccount;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public String getLocation() {
        return this.location;
    }

    public double getNightlyRate() {
        return this.nightlyRate;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public RatingsAggregator getReviewAggregator() {
        return this.ratings;
    }

    public void addRating(int rating) {
        this.ratings.addRating(rating);
    }

    public double getStars() {
        return this.ratings.getStars();
    }

    public HashMap<Integer, CalendarYear> getAvailabilityMap() {
        return availability;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public boolean getAvailability(LocalDate startDate, LocalDate endDate) {
        return AvailabilitySearch.getAvailability(this.availability, startDate, endDate);
    }

    public void toggleAvailability(LocalDate startDate, LocalDate endDate) {
        AvailabilitySearch.toggleAvailability(this.availability, startDate, endDate);
    }

    public void makeAvailable(LocalDate startDate, LocalDate endDate) {
        AvailabilitySearch.makeAvailable(this.availability, startDate, endDate);
    }

    public boolean matchesFilter(String filter, String value) {
        if (value.isEmpty()) return true;

        switch (Filters.valueOf(filter)) {
            case LOCATION:
                return this.location.equals(value);

            case TIME_PERIOD:
                String[] split = value.split("-");
                LocalDate startDate = LocalDate.parse(split[0], DomainUtils.dateFormatter);
                LocalDate endDate = LocalDate.parse(split[1], DomainUtils.dateFormatter);
                return this.getAvailability(startDate, endDate);

            case GUESTS:
                // Only accept amount of guests that are at most smaller by 2
                return this.capacity >= Integer.parseInt(value) &&
                        this.capacity <= Integer.parseInt(value) + 2;

            case NIGHTLY_RATE:
                return this.nightlyRate <= Double.parseDouble(value);

            case STARS:
                return this.getStars() >= Double.parseDouble(value);

            default:
                System.err.println("Rental.matchesFilter(): Filter type not recognized.");
                return false;
        }
    }

    public String toString() {
        return String.format("%s (%s)", this.roomName, this.location);
    }
}
