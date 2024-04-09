package com.homerentals.backend;

import com.homerentals.domain.Rental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapSearch {
    private final HashMap<String, String> filters;
    private final ArrayList<Rental> rentals;

    public MapSearch(HashMap<String, String> filters, ArrayList<Rental> rentals) {
        this.filters = filters;
        this.rentals = rentals;
    }

    public ArrayList<Rental> map() {
       ArrayList<Rental> filteredResults = new ArrayList<>();
        for (Rental rental : rentals) {
            for (Map.Entry<String, String> filter : filters.entrySet()) {
                if (rental.matchesFilter(filter.getKey(), filter.getValue())) {
                    filteredResults.add(rental);
                    break;
                }
            }
        }
        return filteredResults;
    }
}
