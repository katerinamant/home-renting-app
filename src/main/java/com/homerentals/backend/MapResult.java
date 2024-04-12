package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.Rental;

import java.io.Serializable;
import java.util.ArrayList;

public class MapResult implements Serializable {
    private final int mapId;
    private final ArrayList<Rental> rentals;
    private final ArrayList<BookingsByLocation> bookingsByLocation;

    public MapResult(int mapId, ArrayList<Rental> rentals, ArrayList<BookingsByLocation> bookingsByLocation) {
        this.mapId = mapId;
        this.rentals = rentals;
        this.bookingsByLocation = bookingsByLocation;
    }

    public int getMapId() {
        return this.mapId;
    }

    public boolean containsRentals() {
        return this.rentals != null;
    }

    public ArrayList<Rental> getRentals() {
        return this.rentals;
    }

    public ArrayList<BookingsByLocation> getBookingsByLocation() {
        return this.bookingsByLocation;
    }
}
