package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

class RequestHandler implements Runnable {
    private final Socket masterSocket;
    private DataInputStream masterSocketIn = null;

    protected RequestHandler(Socket mastetSocket) throws IOException {
        this.masterSocket = mastetSocket;
        try {
            this.masterSocketIn = new DataInputStream(this.masterSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("RequestHandler(): Error setting up stream: " + e);
            throw e;
        }
    }

    private String readMasterSocketInput() {
        try {
            return this.masterSocketIn.readUTF();
        } catch (IOException e) {
            System.out.println("CLIENT HANDLER: Error reading Client Socket input: " + e);
            return null;
        }
    }

    private Rental jsonToRentalObject(String input) {
        try {
            // Create Rental object from JSON
            JSONObject jsonObject = new JSONObject(input);
            String roomName = jsonObject.getString("roomName");
            String area = jsonObject.getString("area");
            double pricePerNight = jsonObject.getDouble("nightlyRate");
            int numOfPersons = jsonObject.getInt("capacity");
            int numOfReviews = jsonObject.getInt("numOfReviews");
            int sumOfReviews = jsonObject.getInt("sumOfReviews");
            String startDate = jsonObject.getString("startDate");
            String endDate = jsonObject.getString("endDate");
            String imagePath = jsonObject.getString("imagePath");
            int rentalId = jsonObject.getInt("rentalId");
            Rental rental = new Rental(null, roomName, area, pricePerNight,
                    numOfPersons, numOfReviews, sumOfReviews, startDate, endDate,
                    imagePath, rentalId);
            System.out.println(rental.getId());
            return rental;

        } catch (JSONException e) {
            // String is not valid JSON object
            System.out.println("REQUEST HANDLER: Error creating Rental object from JSON: " + e);
            return null;
        }
    }

    @Override
    public void run() {
        String input = null;
        try {
            input = this.readMasterSocketInput();
            if (input == null) {
                System.err.println("RequestHandler.run(): Error reading Master Socket input");
                return;
            }
            System.out.println(input);

            // Handle JSON input
            JSONObject inputJson = new JSONObject(input);
            String inputType = inputJson.getString("type");
            String inputBody = inputJson.getString("body");
            Requests inputHeader = Requests.valueOf(inputJson.getString("header"));

            switch (inputHeader) {
                // Guest Requests
                case GET_RENTALS:
                    // TODO: Return result with MapReduce
                    break;

                case NEW_BOOKING:
                    // TODO
                    break;

                case NEW_RATING:
                    // TODO
                    break;

                // Host Requests
                case NEW_RENTAL:
                    // Create Rental object from JSON
                    Rental rental = this.jsonToRentalObject(inputBody);
                    if (rental == null) {
                        System.out.println("REQUEST HANDLER RUN: Error creating Rental object from JSON");
                        return;
                    }

                    synchronized (Worker.rentals) {
                        System.out.println("lock");
                        System.out.println(Worker.rentals);
                        Worker.rentals.add(rental);
                    }
                    System.out.println("done");
                    System.out.println(Worker.rentals);

                    break;

                case UPDATE_AVAILABILITY:
                    // TODO
                    break;

                case GET_BOOKINGS:
                    // TODO: Return result with MapReduce
                    break;

                default:
                    System.err.println("ClientHandler.run(): Request type not recognized.");
                    break;
            }
        } catch (JSONException e) {
            System.err.println("RequestHandler.run(): Error: " + e);
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing thread");
                this.masterSocketIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
