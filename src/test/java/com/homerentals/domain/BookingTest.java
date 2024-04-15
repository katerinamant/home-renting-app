package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BookingTest {
    @Test
    public void constructor_with_args() {
        GuestAccount guestAccount = new GuestAccount();
        Rental rental = new Rental(null, "Rental", "Athens",
                50.0, 2, 5, 20,"\\path", 0);
        String bookingStartDate = "01/02/2024";
        String bookingEndDate = "03/02/2024";
        Booking booking = new Booking(guestAccount, rental, bookingStartDate, bookingEndDate, "0");

        Assert.assertEquals(guestAccount, booking.getGuestEmail());
        Assert.assertEquals(rental, booking.getRentalId());

        final DateTimeFormatter df = DomainUtils.dateFormatter;
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);
        Assert.assertEquals(startDate, booking.getStartDate());
        Assert.assertEquals(endDate, booking.getEndDate());

        Assert.assertEquals(100.0, booking.getTotalCost(), 0.0);
    }
}
