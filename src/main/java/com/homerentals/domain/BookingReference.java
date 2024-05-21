package com.homerentals.domain;

import com.homerentals.backend.BackendUtils;
import org.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDate;

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

    public String getRentalName() {
        return this.rentalName;
    }

    public String getRentalLocation() {
        return this.rentalLocation;
    }

    public String getDates() {
        return String.format("[%s - %s]", DomainUtils.dateFormatter.format(this.startDate), DomainUtils.dateFormatter.format(this.endDate));
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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put(BackendUtils.BODY_FIELD_RENTAL_ID, this.getRentalId());
        json.put(BackendUtils.BODY_FIELD_BOOKING_ID, this.getBookingId());
        json.put(BackendUtils.BODY_FIELD_RENTAL_NAME, this.getRentalName());
        json.put(BackendUtils.BODY_FIELD_RENTAL_LOCATION, this.getRentalLocation());
        json.put(BackendUtils.BODY_FIELD_BOOKING_DATES_STRING, this.getDates());
        return json;
    }

    @Override
    public String toString() {
        return String.format("Your stay at %s in %s [%s - %s]",
                this.rentalName, this.rentalLocation, DomainUtils.dateFormatter.format(this.startDate), DomainUtils.dateFormatter.format(this.endDate));
    }
}
