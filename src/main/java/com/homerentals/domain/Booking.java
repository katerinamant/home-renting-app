package com.homerentals.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Booking {
    private final GuestAccount guest;
    private final Rental rental;

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private final LocalDate startDate;
    private final LocalDate endDate;

    private final double totalCost;

    public Booking(GuestAccount guest, Rental rental, String startDate, String endDate) {
        this.guest = guest;
        this.rental = rental;
        this.startDate = LocalDate.parse(startDate, df);
        this.endDate = LocalDate.parse(endDate, df);
        this.totalCost = this.rental.getNightlyRate() * (this.endDate.getDayOfMonth() - this.startDate.getDayOfMonth());
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
        return this.totalCost;
    }
}