package com.homerentals.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public class CalendarYear {
    private final int year;
    private final boolean[] availability;
    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    protected CalendarYear(int year) {
        this.year = year;
        int days = 365;
        days += (this.year % 4 == 0) ? 1 : 0;
        this.availability = new boolean[days];
        Arrays.fill(this.availability, false);
    }

    public int getYear() {
        return year;
    }

    public boolean[] getAvailability() {
        return this.availability;
    }

    protected boolean isAvailable(LocalDate date) {
        int index = date.getDayOfYear() - 1;
        return this.availability[index];
    }

    protected void toggleAvailability(LocalDate date) {
        int index = date.getDayOfYear() - 1;
        this.availability[index] = !this.availability[index];
    }
}