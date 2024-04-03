package com.homerentals.domain;

import java.time.LocalDate;
import java.util.HashMap;

public class AvailabilitySearch {
    protected static boolean getAvailability(HashMap<Integer, Calendar> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int year;
        Calendar calendar;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                return false;

            calendar = availability.get(year);
            if (!calendar.isAvailable(date))
                return false;
        }

        // Checking for endDate
        year = endDate.getYear();
        if (!availability.containsKey(year))
            return false;

        calendar = availability.get(year);
        return calendar.isAvailable(endDate);
    }

    protected static void toggleAvailability(HashMap<Integer, Calendar> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int year;
        Calendar calendar;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            year = date.getYear();
            if (!availability.containsKey(year))
                availability.put(year, new Calendar(year));

            calendar = availability.get(year);
            calendar.toggleAvailability(date);
        }

        // Toggling endDate
        year = endDate.getYear();
        if (!availability.containsKey(year))
            availability.put(year, new Calendar(year));

        calendar = availability.get(year);
        calendar.toggleAvailability(endDate);
    }
}
