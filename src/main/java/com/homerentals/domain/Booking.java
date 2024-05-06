package com.homerentals.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class Booking implements Serializable {
    private final String bookingId;
    private final int rentalId;
    private final String guestEmail;

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double rentalNightlyRate;

    public Booking(String bookingId, int rentalId, String guestEmail, String startDate, String endDate, double rentalNightlyRate) {
        this.bookingId = bookingId;
        this.rentalId = rentalId;
        this.guestEmail = guestEmail;
        this.startDate = LocalDate.parse(startDate, DomainUtils.dateFormatter);
        this.endDate = LocalDate.parse(endDate, DomainUtils.dateFormatter);
        this.rentalNightlyRate = rentalNightlyRate;
    }

    private double calculateTotalCost() {
        return this.rentalNightlyRate * (this.endDate.getDayOfMonth() - this.startDate.getDayOfMonth());
    }

    public String getGuestEmail() {
        return this.guestEmail;
    }

    public int getRentalId() {
        return this.rentalId;
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

    public boolean occursDuring(LocalDate startDate, LocalDate endDate) {
        return this.startDate.isBefore(endDate) && startDate.isBefore(this.endDate);
    }

    public boolean hasPassed() {
        LocalDate today = LocalDate.now();
        return this.endDate.isBefore(today);
    }
}
