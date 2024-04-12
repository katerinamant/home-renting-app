package com.homerentals.backend;

import com.homerentals.domain.Rental;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class MapResult implements Serializable {
    private final int mapId;
    private final ArrayList<Rental> rentals;

    public MapResult(int mapId, ArrayList<Rental> rentals) {
        this.mapId = mapId;
        this.rentals = rentals;
    }

    public int getMapId() {
        return mapId;
    }

    public boolean containsRentals() {
        return !rentals.isEmpty();
    }

    public ArrayList<Rental> getRentals() {
        return rentals;
    }
}
