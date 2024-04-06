package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

public class CalendarYearTest {
    @Test
    public void constructor_with_args() {
        // Not leap year
        CalendarYear CalendarYear = new CalendarYear(2023);

        Assert.assertEquals(2023, CalendarYear.getYear());
        Assert.assertEquals(365, CalendarYear.getAvailability().length);
        Assert.assertFalse(Collections.singletonList(CalendarYear.getAvailability()).contains(true));

        // Leap year
        CalendarYear = new CalendarYear(2024);
        Assert.assertEquals(2024, CalendarYear.getYear());
        Assert.assertEquals(366, CalendarYear.getAvailability().length);
        Assert.assertFalse(Collections.singletonList(CalendarYear.getAvailability()).contains(true));
    }

    @Test
    public void availability() {
        CalendarYear CalendarYear = new CalendarYear(2024);

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
        LocalDate localDate = LocalDate.parse("01/01/2024", df);

        Assert.assertFalse(CalendarYear.isAvailable(localDate));
        CalendarYear.toggleAvailability(localDate);
        Assert.assertTrue(CalendarYear.isAvailable(localDate));
    }
}
