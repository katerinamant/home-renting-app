package com.homerentals.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class Rental {
    private final HostAccount hostAccount;
    private final String roomName;
    private final String area;
    private final double nightlyRate;
    private final int capacity;
    private final RatingsAggregator ratings;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final HashMap<Integer, CalendarYear> availability;

    private final String imagePath;

    public Rental(HostAccount hostAccount,
                  String roomName,
                  String area,
                  double nightlyRate,
                  int capacity,
                  int numOfReviews,
                  int sumOfReviews,
                  String startDate,
                  String endDate,
                  String imagePath) {
        this.hostAccount = hostAccount;
        this.roomName = roomName;
        this.area = area;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
        this.ratings = new RatingsAggregator(numOfReviews, sumOfReviews);
        this.startDate = LocalDate.parse(startDate, dateFormatter);
        this.endDate = LocalDate.parse(endDate, dateFormatter);
        this.imagePath = imagePath;

        // All dates are initialised to unavailable.
        // Dates spanning from startDate to endDate
        // are toggled.
        this.availability = new HashMap<Integer, CalendarYear>();
        int year;
        CalendarYear calendar;
        for (LocalDate date = this.startDate; date.isBefore(this.endDate.plusDays(1)); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                availability.put(year, new CalendarYear(year));

            calendar = availability.get(year);
            calendar.toggleAvailability(date);
        }
    }

    public HostAccount getHostAccount() {
        return this.hostAccount;
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

    public RatingsAggregator getReviewAggregator() {
        return this.ratings;
    }

    public void addRating(int rating) {
        this.ratings.addRating(rating);
    }

    public double getStars() {
        return this.ratings.getStars();
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
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
}
