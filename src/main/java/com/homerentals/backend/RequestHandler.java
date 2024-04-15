package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.Filters;
import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;

class RequestHandler implements Runnable {
    private final Socket serverSocket;
    private DataInputStream serverSocketIn = null;
    private DataOutputStream serverSocketOut = null;

    protected RequestHandler(Socket serverSocket) throws IOException {
        this.serverSocket = serverSocket;
        try {
            this.serverSocketOut = new DataOutputStream(this.serverSocket.getOutputStream());
            this.serverSocketIn = new DataInputStream(this.serverSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("RequestHandler(): Error setting up stream: " + e);
            throw e;
        }
    }

    private String readServerSocketInput() {
        try {
            return this.serverSocketIn.readUTF();
        } catch (IOException e) {
            System.err.println("RequestHandler.readServerSocketInput(): Error reading Client Socket input: " + e);
            return null;
        }
    }

    private void sendServerSocketOutput(String msg) throws IOException {
        try {
            this.serverSocketOut.writeUTF(msg);
            this.serverSocketOut.flush();
        } catch (IOException e) {
            System.err.println("RequestHandler.sendServerSocketOutput(): Error sending Socket Output: " + e.getMessage());
            throw e;
        }
    }

    private LocalDate[] parseJsonDates(JSONObject json) {
        String startDateString = json.getString(BackendUtils.BODY_FIELD_START_DATE);
        String endDateString = json.getString(BackendUtils.BODY_FIELD_END_DATE);
        LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(startDateString, BackendUtils.dateFormatter);
            endDate = LocalDate.parse(endDateString, BackendUtils.dateFormatter);
        } catch (DateTimeParseException e) {
            System.err.printf("RequestHandler.parseJsonDates(): Error parsing dates: %s and %s%n", startDateString, endDateString);
            return null;
        }
        return new LocalDate[]{startDate, endDate};
    }

    private void sendMappingToReducer(int mapId, ArrayList<Rental> rentals, ArrayList<BookingsByLocation> bookingsByLocation) {
        // Wrap results in object
        MapResult results = new MapResult(mapId, rentals, bookingsByLocation);

        // Send results to reducer
        try {
            Worker.writeToReducerSocket(results);
        } catch (IOException e) {
            System.err.println("RequestHandler.sendMappingToReducer(): Error writing to Reducer Socket: " + e);
        }
    }

    @Override
    public void run() {
        String input = null;
        try {
            input = this.readServerSocketInput();
            if (input == null) {
                System.err.println("RequestHandler.run(): Error reading Master Socket input");
                return;
            }
            System.out.println(input);

            // Handle JSON input
            JSONObject inputJson = new JSONObject(input);
            String inputType = inputJson.getString(BackendUtils.MESSAGE_TYPE);
            JSONObject inputBody = new JSONObject(inputJson.getString(BackendUtils.MESSAGE_BODY));
            Requests inputHeader = Requests.valueOf(inputJson.getString(BackendUtils.MESSAGE_HEADER));

            Rental rental;
            LocalDate[] dates;
            LocalDate startDate, endDate;
            int rentalId, mapId;
            String bookingId;
            Mapper mapper = new Mapper(Worker.rentals);
            switch (inputHeader) {
                // Guest Requests
                case GET_RENTALS:
                    // Parse JSON Message
                    mapId = inputBody.getInt(BackendUtils.BODY_FIELD_MAP_ID);
                    JSONObject jsonFilters = inputBody.getJSONObject(BackendUtils.BODY_FIELD_FILTERS);

                    System.out.println("> Received filters in JSON: " + jsonFilters);

                    // Create filters hashmap
                    HashMap<String, String> filters = new HashMap<>();

                    if (jsonFilters.isEmpty()) {
                        for (Filters f : Filters.values()) {
                            String filterName = f.name();
                            filters.put(filterName, "");
                            System.out.printf("Storing filter: [%s = %s]%n", filterName, "");
                        }
                    } else {
                        // Iterate over each filter in Filters enum
                        for (Filters f : Filters.values()) {
                            String filterName = f.name();
                            if (jsonFilters.has(filterName)) {
                                String filterValue = jsonFilters.getString(filterName);
                                filters.put(filterName, filterValue);
                                System.out.printf("Storing filter: [%s = %s]%n", filterName, filterValue);
                            }
                        }
                    }

                    System.out.println("Created filters map: " + filters);

                    // Perform mapping
                    ArrayList<Rental> mappedRentals = mapper.mapRentalsToFilters(filters);

                    // Send to reducer
                    this.sendMappingToReducer(mapId, mappedRentals, null);
                    break;

                case NEW_BOOKING:
                    // Parse JSON Object
                    int requestId = inputBody.getInt(BackendUtils.BODY_FIELD_REQUEST_ID);
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    bookingId = inputBody.getString(BackendUtils.BODY_FIELD_BOOKING_ID);
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("RequestHandler.run(): Error parsing dates");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];

                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.printf("RequestHandler.run(): Rental with ID %d not found%n", rentalId);
                        break;
                    }

                    boolean successfulBooking = false;
                    synchronized (rental) {
                        System.out.println("lock");
                        if (rental.getAvailability(startDate, endDate)) {
                            // Execute booking
                            successfulBooking = true;

                            String startDateString = BackendUtils.dateFormatter.format(startDate);
                            String endDateString = BackendUtils.dateFormatter.format(endDate);
                            Booking booking = new Booking(null, rental, startDateString, endDateString, bookingId);
                            rental.addBooking(booking);
                            // TODO: Add booking to guest's list
                        }
                    }
                    System.out.println("done");
                    System.out.println(rental.getAvailability(startDate, endDate));

                    // Send response to Server
                    JSONObject responseBody = new JSONObject();
                    responseBody.put(BackendUtils.BODY_FIELD_STATUS, successfulBooking ? "OK" : "ERROR");
                    JSONObject response = BackendUtils.createResponse(inputHeader.name(), responseBody.toString(), requestId);
                    System.out.printf("Sending response for requestId [%d]: %s%n", requestId, response);
                    this.sendServerSocketOutput(response.toString());
                    break;

                case NEW_RATING:
                    // Parse JSON Object
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.printf("RequestHandler.run(): Rental with ID %d not found%n", rentalId);
                        break;
                    }

                    int rating = inputBody.getInt(BackendUtils.BODY_FIELD_RATING);
                    synchronized (rental) {
                        System.out.println("lock");
                        System.out.println(rental.getStars());
                        rental.addRating(rating);
                        // TODO: Remove booking from guest's list
                    }
                    System.out.println("done");
                    System.out.println(rental.getStars());
                    break;

                // Host Requests
                case NEW_RENTAL:
                    // Create Rental object from JSON
                    rental = BackendUtils.jsonToRentalObject(inputBody);
                    if (rental == null) {
                        System.err.println("RequestHandler.run(): Error creating Rental object from JSON");
                        return;
                    }

                    synchronized (Worker.rentals) {
                        System.out.println("lock");
                        System.out.println(Worker.rentals);
                        Worker.rentals.add(rental);
                        Worker.idToRental.put(rental.getId(), rental);
                    }
                    System.out.println("done");
                    System.out.println(Worker.rentals);

                    break;

                case UPDATE_AVAILABILITY:
                    // Get Rental object from rentalId
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.println("RequestHandler.run(): Rental with ID " + rentalId + " not found");
                        break;
                    }

                    // Get LocalDate objects
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("RequestHandler.run(): Error parsing dates");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];
                    synchronized (rental) {
                        System.out.println("lock");
                        System.out.println(rental.getAvailability(startDate, endDate));
                        // TODO: Add check,
                        //  don't allow action if it is unavailable because of a booking
                        rental.makeAvailable(startDate, endDate);
                    }
                    System.out.println("done");
                    System.out.println(rental.getAvailability(startDate, endDate));
                    break;

                case GET_BOOKINGS:
                    // Parse JSON Message
                    mapId = inputBody.getInt(BackendUtils.BODY_FIELD_MAP_ID);
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("RequestHandler.run(): Error parsing dates");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];

                    // Perform mapping
                    ArrayList<BookingsByLocation> bookingsByLocations = mapper.mapBookingsToLocations(startDate, endDate);

                    // Send to reducer
                    for (BookingsByLocation bbl : bookingsByLocations) {
                        System.out.println("Sending to reducer: " + bbl.getBookingIds());
                    }
                    this.sendMappingToReducer(mapId, null, bookingsByLocations);
                    break;

                default:
                    System.err.println("RequestHandler.run(): Request type not recognized.");
                    break;
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("RequestHandler.run(): Error: " + e);
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing thread");
                this.serverSocketIn.close();
                this.serverSocketOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
