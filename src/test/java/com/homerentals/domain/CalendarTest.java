package com.homerentals.domain;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public class CalendarTest {
    @Test
    public void constructor_with_args() {
        // Not leap year
        Calendar calendar = new Calendar(2023);

        Assert.assertTrue(2023 == calendar.getYear());
        Assert.assertTrue(365 == calendar.getAvailability().length);
        Assert.assertFalse(Arrays.asList(calendar.getAvailability()).contains(true));

        // Leap year
        calendar = new Calendar(2024);
        Assert.assertTrue(2024 == calendar.getYear());
        Assert.assertTrue(366 == calendar.getAvailability().length);
        Assert.assertFalse(Arrays.asList(calendar.getAvailability()).contains(true));
    }

    @Test
    public void availability() {
        Calendar calendar = new Calendar(2024);

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
        LocalDate localDate = LocalDate.parse("01/01/2024", df);

        Assert.assertFalse(calendar.isAvailable(localDate));
        calendar.toggleAvailability(localDate);
        Assert.assertTrue(calendar.isAvailable(localDate));
    }
}
