package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class RequestHandler implements Runnable {
    private final ArrayList<Rental> rentals;
    private final JSONObject requestJson;

    protected RequestHandler(ArrayList<Rental> rentals, JSONObject requestJson) {
        this.rentals = rentals;
        this.requestJson = requestJson;
    }

    private Rental jsonToRentalObject(String input) {
        try {
            // Create Rental object from JSON
            JSONObject jsonObject = new JSONObject(input);
            String roomName = jsonObject.getString("roomName");
            String area = jsonObject.getString("area");
            double pricePerNight = jsonObject.getDouble("pricePerNight");
            int numOfPersons = jsonObject.getInt("numOfPersons");
            int numOfReviews = jsonObject.getInt("numOfReviews");
            int sumOfReviews = jsonObject.getInt("sumOfReviews");
            String startDate = jsonObject.getString("startDate");
            String endDate = jsonObject.getString("endDate");
            String imagePath = jsonObject.getString("imagePath");
            Rental rental = new Rental(null, roomName, area, pricePerNight,
                    numOfPersons, numOfReviews, sumOfReviews, startDate, endDate, imagePath);
            return rental;

        } catch (JSONException e) {
            // String is not valid JSON object
            System.out.println("REQUEST HANDLER: Error creating Rental object from JSON: " + e);
            return null;
        }
    }

    @Override
    public void run() {
        // Handle JSON input
        String inputType = this.requestJson.getString("type");
        String inputHeader = this.requestJson.getString("header");
        String inputBody = this.requestJson.getString("body");

        if (inputType.equals("request") && inputHeader.equals("new-rental")) {
            // Create Rental object from JSON
            Rental rental = this.jsonToRentalObject(inputBody);
            if (rental == null) {
                System.out.println("REQUEST HANDLER RUN: Error creating Rental object from JSON");
                return;
            }

            synchronized (this.rentals) {
                System.out.println("lock");
                System.out.println(this.rentals);
                this.rentals.add(rental);
            }
            System.out.println("done");
            System.out.println(this.rentals);
        }
    }
}
