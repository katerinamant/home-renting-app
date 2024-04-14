package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.Rental;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Mapper {
    private final ArrayList<Rental> rentals;

    public Mapper(ArrayList<Rental> rentals) {
        this.rentals = rentals;
    }

    public ArrayList<Rental> mapRentalsToFilters(HashMap<String, String> filters) {
        ArrayList<Rental> results = new ArrayList<>();
        for (Rental rental : rentals) {
            boolean matchesAll = true;
            for (Map.Entry<String, String> filter : filters.entrySet()) {
                if (!rental.matchesFilter(filter.getKey(), filter.getValue())) {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll) {
                results.add(rental);
            }
        }
        return results;
    }

    public ArrayList<BookingsByLocation> mapBookingsToLocations(LocalDate startDate, LocalDate endDate) {
        HashMap<String, BookingsByLocation> bookings = new HashMap<>();
        for (Rental rental : rentals) {
            // Get the object for storing bookings associated with rental's location
            String location = rental.getLocation();
            if (!bookings.containsKey(location)) {
                bookings.put(location, new BookingsByLocation(location));
            }
            BookingsByLocation bookingsByLocation = bookings.get(location);

            // Iterate over all bookings for this rental and add them to the object
            // if they happen during the period given by the user
            for (Booking booking : rental.getBookings()) {
                if (isInDateRange(booking, startDate, endDate)) {
                    bookingsByLocation.addBooking(booking.getBookingId());
                }
            }
        }
        return new ArrayList<>(bookings.values());
    }

    private boolean isInDateRange(Booking booking, LocalDate startDate, LocalDate endDate) {
        LocalDate bookingStart = booking.getStartDate();
        LocalDate bookingEnd = booking.getEndDate();
        return bookingStart.isAfter(startDate.minusDays(1)) && bookingEnd.isBefore(endDate.plusDays(1));
    }
}
