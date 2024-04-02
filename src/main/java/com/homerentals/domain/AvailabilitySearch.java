package com.homerentals.domain;

import java.time.LocalDate;
import java.util.HashMap;

public class AvailabilitySearch {
    protected static boolean getAvailability(HashMap<Integer, Calendar> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int key;
        Calendar calendar;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            key = date.getYear();
            if (!availability.containsKey(key))
                return false;

            calendar = availability.get(key);
            if (!calendar.isAvailable(date))
                return false;
        }

        // Checking for endDate
        key = endDate.getYear();
        if (!availability.containsKey(key))
            return false;

        calendar = availability.get(key);
        return calendar.isAvailable(endDate);
    }

    protected static void toggleAvailability(HashMap<Integer, Calendar> availability, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date input");
        }

        int key;
        Calendar calendar;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            key = date.getYear();
            if (!availability.containsKey(key))
                availability.put(key, new Calendar(key));

            calendar = availability.get(key);
            calendar.toggleAvailability(date);
        }

        // Toggling endDate
        key = endDate.getYear();
        if (!availability.containsKey(key))
            availability.put(key, new Calendar(key));

        calendar = availability.get(key);
        calendar.toggleAvailability(endDate);
    }
}
