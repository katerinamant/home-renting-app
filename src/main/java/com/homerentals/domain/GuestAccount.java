package com.homerentals.domain;

import java.time.LocalDate;
import java.util.ArrayList;

public class GuestAccount extends AbstractAccount {
    private final ArrayList<BookingReference> bookingReferences;

    protected GuestAccount() {
        super();
        this.bookingReferences = new ArrayList<>();
    }

    public GuestAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
        this.bookingReferences = new ArrayList<>();
    }

    public ArrayList<BookingReference> getUnratedBookings() {
        ArrayList<BookingReference> bookingReferences = new ArrayList<>();
        for (BookingReference bookingReference : this.bookingReferences) {
            if (bookingReference.hasPassed() && !bookingReference.hasRating()) {
                bookingReferences.add(bookingReference);
            }
        }
        return bookingReferences;
    }

    public void addBooking(String bookingId, int rentalId, String rentalName, String rentalLocation, LocalDate startDate, LocalDate endDate) {
        BookingReference bookingReference = new BookingReference(bookingId, rentalId, rentalName, rentalLocation, startDate, endDate);
        this.bookingReferences.add(bookingReference);
    }

    public void rateBooking(String bookingId) {
        for (BookingReference bookingReference : this.bookingReferences) {
            if (bookingReference.getBookingId().equals(bookingId)) {
                bookingReference.rate();
                return;
            }
        }
    }
}
