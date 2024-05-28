package com.homerentals.domain;

import com.homerentals.backend.BackendUtils;
import org.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public class Rental implements Serializable {
    private final int id;
    private final String imgUrl;
    private final HostAccount hostAccount;
    private final String roomName;
    private final String location;
    private final double nightlyRate;
    private final int capacity;
    private final RatingsAggregator ratings;
    private final ArrayList<Booking> bookings;

    private final HashMap<Integer, CalendarYear> availability;

    public Rental(
            HostAccount hostAccount,
            String roomName,
            String location,
            double nightlyRate,
            int capacity,
            int numOfRatings,
            int sumOfRatings,
            String imgUrl,
            int id
    ) {
        this.hostAccount = hostAccount;
        this.roomName = roomName;
        this.location = location;
        this.nightlyRate = nightlyRate;
        this.capacity = capacity;
        this.ratings = new RatingsAggregator(numOfRatings, sumOfRatings);
        this.imgUrl = imgUrl;
        this.id = id;
        this.bookings = new ArrayList<>();
        this.availability = new HashMap<>();
    }

    public int getId() {
        return this.id;
    }

    public String getImageURL() { return this.imgUrl; }

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

    public ArrayList<Booking> getBookings() {
        return this.bookings;
    }

    public void addBooking(Booking booking) {
        this.bookings.add(booking);
        toggleAvailability(booking.getStartDate(), booking.getEndDate());
    }

    public boolean getAvailability(LocalDate startDate, LocalDate endDate) {
        return AvailabilitySearch.getAvailability(this.availability, startDate, endDate);
    }

    public void toggleAvailability(LocalDate startDate, LocalDate endDate) {
        AvailabilitySearch.toggleAvailability(this.availability, startDate, endDate);
    }

    public boolean makeAvailable(LocalDate startDate, LocalDate endDate) {
        // Do not allow this action
        // if a booking occurs during
        // this time period
        for (Booking booking : this.bookings) {
            if (booking.occursDuring(startDate, endDate)) {
                return false;
            }
        }
        AvailabilitySearch.makeAvailable(this.availability, startDate, endDate);
        return true;
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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put(BackendUtils.BODY_FIELD_RENTAL_ID, this.getId());
        json.put(BackendUtils.BODY_FIELD_RENTAL_NAME, this.getRoomName());
        json.put(BackendUtils.BODY_FIELD_RENTAL_LOCATION, this.getLocation());
        json.put(BackendUtils.BODY_FIELD_RENTAL_NIGHTLY_RATE, this.getNightlyRate());
        json.put(BackendUtils.BODY_FIELD_RENTAL_CAPACITY, this.getCapacity());
        json.put(BackendUtils.BODY_FIELD_RENTAL_STARS, this.getStars());
        json.put(BackendUtils.BODY_FIELD_RENTAL_STRING, this.toString());
        json.put(BackendUtils.BODY_FIELD_RENTAL_IMAGE_URL, this.getImageURL());
        return json;
    }

    public String toString() {
        return String.format("%s (%s)", this.roomName, this.location);
    }
}
