package com.homerentals.domain;

import java.time.LocalDate;
import java.util.HashMap;

public class AvailabilitySearch {
    protected static boolean getAvailability(HashMap<Integer, CalendarYear> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int year;
        CalendarYear CalendarYear;
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                return false;

            CalendarYear = availability.get(year);
            if (!CalendarYear.isAvailable(date))
                return false;
        }

        return true;
    }

    protected static void toggleAvailability(HashMap<Integer, CalendarYear> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int year;
        CalendarYear CalendarYear;
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                availability.put(year, new CalendarYear(year));

            CalendarYear = availability.get(year);
            CalendarYear.toggleAvailability(date);
        }
    }

    public static void makeAvailable(HashMap<Integer, CalendarYear> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int year;
        CalendarYear CalendarYear;
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                availability.put(year, new CalendarYear(year));

            CalendarYear = availability.get(year);
            if(!CalendarYear.isAvailable(date)) {
                CalendarYear.toggleAvailability(date);
            }
        }
    }
}
