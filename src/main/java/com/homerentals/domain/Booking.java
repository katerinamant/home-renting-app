package com.homerentals.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Booking {
    private final GuestAccount guest;
    private final Rental rental;

    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
    private final Date startDate;
    private final Date endDate;

    private final double totalCost;

    public Booking(GuestAccount guest, Rental rental, String startDate, String endDate) {
        this.guest = guest;
        this.rental = rental;
        try {
            this.startDate = df.parse(startDate);
            this.endDate = df.parse(endDate);

            this.totalCost = this.rental.getPricePerNight() * (this.endDate.getDay() - this.startDate.getDay());
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public GuestAccount getGuest() {
        return this.guest;
    }

    public Rental getRental() {
        return this.rental;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public double getTotalCost() {
        return this.totalCost;
    }
}