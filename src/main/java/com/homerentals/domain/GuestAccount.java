package com.homerentals.domain;

import java.util.ArrayList;

public class GuestAccount extends AbstractAccount {
    private ArrayList<Booking> bookingsWithNoRating;
    protected GuestAccount() {
        super();
    }

    public GuestAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
        this.bookingsWithNoRating = new ArrayList<>();
    }

    public ArrayList<Booking> getBookingsWithNoRating() {
        return this.bookingsWithNoRating;
    }

    public void addBooking(Booking booking) {
        this.bookingsWithNoRating.add(booking);
    }
}
