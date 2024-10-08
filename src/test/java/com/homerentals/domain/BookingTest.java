package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BookingTest {
    @Test
    public void constructor_with_args() {
        Rental rental = new Rental(null, "Rental", "Athens",
                50.0, 2, 5, 20, "", 0);
        String bookingStartDate = "01/02/2024";
        String bookingEndDate = "03/02/2024";
        Booking booking = new Booking("0", rental.getId(),"guest@example.com", bookingStartDate, bookingEndDate, rental.getNightlyRate());

        Assert.assertEquals("guest@example.com", booking.getGuestEmail());
        Assert.assertEquals(rental.getId(), booking.getRentalId());

        final DateTimeFormatter df = DomainUtils.dateFormatter;
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);
        Assert.assertEquals(startDate, booking.getStartDate());
        Assert.assertEquals(endDate, booking.getEndDate());

        Assert.assertEquals(100.0, booking.getTotalCost(), 0.0);
    }
}
