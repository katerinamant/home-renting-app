package com.homerentals.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

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
        if (this.startDate.isEqual(startDate) || this.endDate.isEqual(endDate) ||
                this.startDate.isEqual(endDate) || this.endDate.isEqual(startDate)){
            // Start date or end date matches
            // that of the time period
            return true;
        }

        if (this.startDate.isAfter(startDate) || this.endDate.isBefore(endDate)) {
            // The booking occurs entirely
            // within the time period
            return true;
        }

        if (this.startDate.isBefore(startDate) || this.endDate.isAfter(endDate)) {
            // The booking is ongoing
            // during the time period
            return true;
        }

        // If part of the booking occurs
        // within the time period, return true
        return (this.startDate.isBefore(startDate) && this.endDate.isAfter(startDate) && this.endDate.isBefore(endDate)) ||
                (this.startDate.isAfter(startDate) && this.startDate.isBefore(endDate) && this.endDate.isAfter(endDate));
    }
}
