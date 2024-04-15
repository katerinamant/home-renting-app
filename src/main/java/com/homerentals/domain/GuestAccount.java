package com.homerentals.domain;

public class GuestAccount extends AbstractAccount {
    private final BookingsReference bookingsReference;

    protected GuestAccount() {
        super();
        this.bookingsReference = new BookingsReference();
    }

    public GuestAccount(Email email, Password password, String firstName, String lastName, PhoneNumber phoneNumber) {
        super(email, password, firstName, lastName, phoneNumber);
        this.bookingsReference = new BookingsReference();
    }

    public BookingsReference getBookingsReference() {
        return this.bookingsReference;
    }

    public void addBooking(String bookingId, int rentalId) {
        this.bookingsReference.addBooking(bookingId, rentalId);
    }
}
