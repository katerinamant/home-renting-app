package com.homerentals.domain;

import java.time.LocalDate;

public class Booking {
    private final GuestAccount guest;
    private final Rental rental;

    private final LocalDate startDate;
    private final LocalDate endDate;

    private final double totalCost;

    public Booking(GuestAccount guest, Rental rental, String startDate, String endDate) {
        this.guest = guest;
        this.rental = rental;
        this.startDate = LocalDate.parse(startDate, DomainUtils.dateFormatter);
        this.endDate = LocalDate.parse(endDate, DomainUtils.dateFormatter);
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