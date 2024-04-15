package com.homerentals.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class Booking implements Serializable {

    private final GuestAccount guest;
    private final Rental rental;
    private final String bookingId;

    private final LocalDate startDate;
    private final LocalDate endDate;

    public Booking(GuestAccount guest, Rental rental, String startDate, String endDate, String bookingId) {
        this.guest = guest;
        this.rental = rental;
        this.startDate = LocalDate.parse(startDate, DomainUtils.dateFormatter);
        this.endDate = LocalDate.parse(endDate, DomainUtils.dateFormatter);
        this.bookingId = bookingId;
    }

    private double calculateTotalCost() {
        return this.rental.getNightlyRate() * (this.endDate.getDayOfMonth() - this.startDate.getDayOfMonth());
    }

    public GuestAccount getGuest() {
        return this.guest;
    }

    public Rental getRental() {
        return this.rental;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public double getTotalCost() {
        return this.calculateTotalCost();
    }

    public String getBookingId() {
        return this.bookingId;
    }
}
