package com.homerentals.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingReference implements Serializable {
    private final String bookingId;
    private final int rentalId;
    private final String rentalName;
    private final String rentalLocation;
    private final LocalDate startDate, endDate;
    private boolean hasRating;

    public BookingReference(String bookingId, int rentalId, String rentalName, String rentalLocation, LocalDate startDate, LocalDate endDate) {
        this.bookingId = bookingId;
        this.rentalId = rentalId;
        this.rentalName = rentalName;
        this.rentalLocation = rentalLocation;
        this.startDate = startDate;
        this.endDate = endDate;
        this.hasRating = false;
    }

    public String getBookingId() {
        return this.bookingId;
    }

    public int getRentalId() {
        return this.rentalId;
    }

    public boolean hasPassed() {
        LocalDate today = LocalDate.now();
        return this.endDate.isBefore(today);
    }

    public boolean hasRating() {
        return this.hasRating;
    }

    public void rate() {
        this.hasRating = true;
    }

    @Override
    public String toString() {
        return String.format("Your stay at %s in %s [%s - %s]",
                this.rentalName, this.rentalLocation, DomainUtils.dateFormatter.format(this.startDate), DomainUtils.dateFormatter.format(this.endDate));
    }
}
