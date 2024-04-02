package com.homerentals.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class Rental {
    private final HostAccount host;
    private final String roomName;
    private final String area;
    private final double nightlyRate;
    private final int capacity;
    private final ReviewAggregator reviews;

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final HashMap<Integer, Calendar> availability;

    private final String imagePath;

    public Rental(HostAccount host, String roomName, String area, double nightlyRate, int capacity, int numOfReviews, int sumOfReviews, String startDate, String endDate, String imagePath) {
        this.host = host;
        this.roomName = roomName;
        this.area = area;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
        this.reviews = new ReviewAggregator(numOfReviews, sumOfReviews);
        this.startDate = LocalDate.parse(startDate, df);
        this.endDate = LocalDate.parse(endDate, df);
        this.imagePath = imagePath;

        // All dates are initialised to unavailable.
        // Dates spanning from startDate to endDate
        // are toggled.
        this.availability = new HashMap<Integer, Calendar>();
        int key;
        Calendar calendar;
        for (LocalDate date = this.startDate; date.isBefore(this.endDate); date = date.plusDays(1)) {
            key = date.getYear();
            if (!availability.containsKey(key))
                availability.put(key, new Calendar(key));

            calendar = availability.get(key);
            calendar.toggleAvailability(date);
        }

        // Toggling endDate
        key = this.endDate.getYear();
        if (!availability.containsKey(key))
            availability.put(key, new Calendar(key));

        calendar = availability.get(key);
        calendar.toggleAvailability(this.endDate);
    }

    public HostAccount getHost() {
        return this.host;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public String getArea() {
        return this.area;
    }

    public double getNightlyRate() {
        return this.nightlyRate;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void addReview(int review) {
        this.reviews.addReview(review);
    }

    public double getStars() {
        return this.reviews.getStars();
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
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
}
