package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class AvailabilitySearchTest {
    Rental rental;
    DateTimeFormatter df;

    @Before
    public void init() {
        rental = new Rental(null, "Rental", "Athens",
                50.0, 2, 5, 20,
                "01/01/2024", "31/12/2025", "\\path");
        df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    }

    @Test
    public void is_available() {
        String bookingStartDate = "31/12/2024";
        String bookingEndDate = "01/01/2025";
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);

        Assert.assertTrue(rental.getAvailability(startDate, endDate));
        rental.toggleAvailability(startDate, endDate);
        Assert.assertFalse(rental.getAvailability(startDate, endDate));
    }

    @Test
    public void is_unavailable() {
        String bookingStartDate = "01/01/2026";
        String bookingEndDate = "02/01/2026";
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);

        HashMap<Integer, CalendarYear> availability = rental.getAvailabilityMap();
        Assert.assertFalse(availability.containsKey(2026));
        Assert.assertFalse(rental.getAvailability(startDate, endDate));

        rental.toggleAvailability(startDate, endDate);
        Assert.assertTrue(availability.containsKey(2026));
        Assert.assertTrue(rental.getAvailability(startDate, endDate));
    }

    @Test
    public void end_date() {
        // The endDate is in a new year
        // and unavailable

        String bookingStartDate = "31/12/2024";
        String bookingEndDate = "01/01/2026";
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);

        HashMap<Integer, CalendarYear> availability = rental.getAvailabilityMap();
        Assert.assertFalse(availability.containsKey(2026));
        Assert.assertFalse(rental.getAvailability(startDate, endDate));

        rental.toggleAvailability(endDate, endDate);
        Assert.assertTrue(availability.containsKey(2026));
        Assert.assertTrue(rental.getAvailability(startDate, endDate));
    }

    @Test(expected = RuntimeException.class)
    public void invalid_date_input() {
        String bookingStartDate = "02/01/2024";
        String bookingEndDate = "01/01/2024";
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);

        rental.getAvailability(startDate, endDate);
    }

    @Test(expected = RuntimeException.class)
    public void invalid_date_input_2() {
        String bookingStartDate = "02/01/2024";
        String bookingEndDate = "01/01/2024";
        LocalDate startDate = LocalDate.parse(bookingStartDate, df);
        LocalDate endDate = LocalDate.parse(bookingEndDate, df);

        rental.toggleAvailability(startDate, endDate);
    }
}
