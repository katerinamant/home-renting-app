package com.homerentals.domain;

import java.util.ArrayList;
import java.time.LocalDate;

public class BookingsReference {
    private final ArrayList<ArrayList<String>> bookings;

    public static boolean isBeforeToday(Booking booking) {
        LocalDate now = LocalDate.now();
        return booking.getStartDate().isBefore(now) && booking.getEndDate().isBefore(now);
    }

    public BookingsReference() {
        this.bookings = new ArrayList<>();
    }

    public ArrayList<ArrayList<String>> getBookings() {
        return this.bookings;
    }

    public void addBooking(String bookingId, int rentalId) {
        ArrayList<String> booking = new ArrayList<>();
        booking.add(bookingId);
        booking.add(String.valueOf(rentalId));
        this.bookings.add(booking);
    }
}
