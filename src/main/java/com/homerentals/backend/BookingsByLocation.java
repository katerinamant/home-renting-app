package com.homerentals.backend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class BookingsByLocation implements Serializable {
    private final String location;
    private final HashSet<String> bookingIds;

    public BookingsByLocation(String location) {
        this.location = location;
        this.bookingIds = new HashSet<>();
    }

    public void addBooking(String bookingId) {
        bookingIds.add(bookingId);
    }

    public void addAll(ArrayList<String> bookingIds) {
        this.bookingIds.addAll(bookingIds);
    }

    public String getLocation() {
        return location;
    }

    public ArrayList<String> getBookingIds() {
        return new ArrayList<>(bookingIds);
    }
}
