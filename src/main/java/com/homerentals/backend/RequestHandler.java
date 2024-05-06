package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.Filters;
import com.homerentals.domain.Rental;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;

class RequestHandler implements Runnable {
    private DataInputStream serverSocketIn = null;
    private DataOutputStream serverSocketOut = null;

    protected RequestHandler(Socket serverSocket) throws IOException {
        try {
            this.serverSocketOut = new DataOutputStream(serverSocket.getOutputStream());
            this.serverSocketIn = new DataInputStream(serverSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("\n! RequestHandler(): Error setting up streams:\n" + e);
            throw e;
        }
    }

    private String readServerSocketInput() {
        try {
            return this.serverSocketIn.readUTF();
        } catch (IOException e) {
            System.err.println("\n! RequestHandler.readServerSocketInput(): Error reading Client Socket input:\n" + e);
            return null;
        }
    }

    private void sendServerSocketOutput(String msg) throws IOException {
        try {
            this.serverSocketOut.writeUTF(msg);
            this.serverSocketOut.flush();
        } catch (IOException e) {
            System.err.println("\n! RequestHandler.sendServerSocketOutput(): Error sending Socket Output:\n" + e);
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
            System.err.printf("\n! RequestHandler.parseJsonDates(): Error parsing dates: %s and %s%n", startDateString, endDateString);
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
            System.err.println("\n! RequestHandler.sendMappingToReducer(): Error writing to Reducer Socket:\n" + e);
        }
    }

    private void handleMapReduceRequest(JSONObject body, Mapper mapper) {
        // Parse JSON Message
        int mapId = body.getInt(BackendUtils.BODY_FIELD_MAP_ID);
        JSONObject jsonFilters = body.getJSONObject(BackendUtils.BODY_FIELD_FILTERS);

        System.out.println("\n> Received filters in JSON: " + jsonFilters + " for mapId #" + mapId);

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

        System.out.println("> Created filters map: " + filters);

        // Perform mapping
        ArrayList<Rental> mappedRentals = mapper.mapRentalsToFilters(filters);

        // Send to reducer
        this.sendMappingToReducer(mapId, mappedRentals, null);
    }

    @Override
    public void run() {
        String input;
        try {
            input = this.readServerSocketInput();
            if (input == null) {
                System.err.println("\n! RequestHandler.run(): Error reading Master Socket input.");
                return;
            }
            System.out.println("\n> Received: " + input);

            // Handle JSON input
            JSONObject inputJson = new JSONObject(input);
            String inputType = inputJson.getString(BackendUtils.MESSAGE_TYPE);
            JSONObject inputBody = new JSONObject(inputJson.getString(BackendUtils.MESSAGE_BODY));
            Requests inputHeader = Requests.valueOf(inputJson.getString(BackendUtils.MESSAGE_HEADER));

            Rental rental;
            JSONObject responseBody, response;
            LocalDate[] dates;
            LocalDate startDate, endDate;
            int rentalId, mapId;
            String bookingId;
            Mapper mapper = new Mapper(Worker.rentals);
            switch (inputHeader) {
                // Guest Requests
                case GET_RENTALS:
                    this.handleMapReduceRequest(inputBody, mapper);
                    break;

                case NEW_BOOKING:
                    // Parse JSON Object
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    bookingId = inputBody.getString(BackendUtils.BODY_FIELD_BOOKING_ID);
                    String email = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("\n! RequestHandler.run(): Error parsing dates.");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];
                    String startDateString = BackendUtils.dateFormatter.format(startDate);
                    String endDateString = BackendUtils.dateFormatter.format(endDate);

                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.printf("\n! RequestHandler.run(): Rental with ID %d not found.%n", rentalId);
                        break;
                    }

                    boolean successfulBooking = false;
                    synchronized (rental) {
                        System.out.println("\n> Lock rental #" + rentalId + " for new booking.");
                        if (rental.getAvailability(startDate, endDate)) {
                            // Execute booking
                            Booking booking = new Booking(bookingId, rentalId, email, startDateString, endDateString, rental.getNightlyRate());
                            rental.addBooking(booking);
                            successfulBooking = true;
                        }
                    }
                    System.out.println(successfulBooking ? "\n> Booking was successful." : "\n> Booking was unsuccessful.");
                    System.out.println("\n> Checking rental #" + rentalId + "'s availability: " + rental.getAvailability(startDate, endDate));

                    // Send response to Server
                    responseBody = new JSONObject();
                    if (successfulBooking) {
                        // Add fields necessary to associate
                        // booking with GuestAccount
                        responseBody.put(BackendUtils.BODY_FIELD_STATUS, "OK");
                        responseBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);
                        responseBody.put(BackendUtils.BODY_FIELD_BOOKING_ID, bookingId);
                        responseBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentalId);
                        responseBody.put(BackendUtils.BODY_FIELD_RENTAL_NAME, rental.getRoomName());
                        responseBody.put(BackendUtils.BODY_FIELD_RENTAL_LOCATION, rental.getLocation());
                        responseBody.put(BackendUtils.BODY_FIELD_START_DATE, startDateString);
                        responseBody.put(BackendUtils.BODY_FIELD_END_DATE, endDateString);
                    } else {
                        responseBody.put(BackendUtils.BODY_FIELD_STATUS, "ERROR");
                    }
                    response = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                    System.out.printf("\n> Sending response : %s%n", response);
                    this.sendServerSocketOutput(response.toString());
                    break;

                case NEW_RATING:
                    // Parse JSON Object
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.printf("\n! RequestHandler.run(): Rental with ID %d not found.%n", rentalId);
                        // Send response to Server
                        responseBody = new JSONObject();
                        responseBody.put(BackendUtils.BODY_FIELD_STATUS, "ERROR");
                        break;
                    }

                    int rating = inputBody.getInt(BackendUtils.BODY_FIELD_RATING);
                    synchronized (rental) {
                        System.out.println("\n> Lock rental #" + rentalId + " for new rating.");
                        System.out.println("\n> Previous rating: " + rental.getStars());
                        rental.addRating(rating);
                    }
                    System.out.println("\n> Rating complete.");
                    System.out.println("\n> Checking rental #" + rentalId + "'s stars: " + rental.getStars());

                    // Send response to Server.
                    // Add fields necessary to mark
                    // booking as rated in the guest's account
                    responseBody = new JSONObject();
                    responseBody.put(BackendUtils.BODY_FIELD_STATUS, "OK");
                    responseBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL));
                    responseBody.put(BackendUtils.BODY_FIELD_BOOKING_ID, inputBody.getString(BackendUtils.BODY_FIELD_BOOKING_ID));
                    response = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                    System.out.printf("\n> Sending response: %s%n", response);
                    this.sendServerSocketOutput(response.toString());
                    break;

                // Host Requests
                case NEW_RENTAL:
                    // Create Rental object from JSON
                    rental = BackendUtils.jsonToRentalObject(inputBody);
                    if (rental == null) {
                        System.err.println("\n! RequestHandler.run(): Error creating Rental object from JSON");
                        return;
                    }

                    synchronized (Worker.rentals) {
                        System.out.println("\n> Lock rentals list for new rental.");
                        System.out.println("\n> Rentals list before: " + Worker.rentals);
                        Worker.rentals.add(rental);
                        Worker.idToRental.put(rental.getId(), rental);
                    }
                    System.out.println("\n> New rental complete.");
                    System.out.println("\n> Rentals list after: " + Worker.rentals);

                    break;

                case UPDATE_AVAILABILITY:
                    // Parse JSON object
                    rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    rental = Worker.idToRental.get(rentalId);
                    if (rental == null) {
                        System.err.println("\n! RequestHandler.run(): Rental with ID " + rentalId + " not found.");
                        break;
                    }

                    // Get LocalDate objects
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("\n! RequestHandler.run(): Error parsing dates.");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];

                    boolean successfulChange;
                    synchronized (rental) {
                        System.out.println("\n> Lock rental #" + rentalId + " for update availability.");
                        successfulChange = rental.makeAvailable(startDate, endDate);
                    }
                    System.out.println(successfulChange ? "\n> Update was successful." : "\n> Update was unsuccessful.");
                    System.out.println("\n> Checking rental #" + rentalId + "'s availability: " + rental.getAvailability(startDate, endDate));

                    // Send response to Server
                    responseBody = new JSONObject();
                    responseBody.put(BackendUtils.BODY_FIELD_STATUS, successfulChange ? "OK" : "ERROR");
                    response = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                    System.out.printf("\n> Sending response: %s%n", response);
                    this.sendServerSocketOutput(response.toString());
                    break;

                case GET_ALL_BOOKINGS:
                    this.handleMapReduceRequest(inputBody, mapper);
                    break;

                case GET_BOOKINGS_BY_LOCATION:
                    // Parse JSON Message
                    mapId = inputBody.getInt(BackendUtils.BODY_FIELD_MAP_ID);
                    dates = this.parseJsonDates(inputBody);
                    if (dates == null) {
                        System.err.println("\n! RequestHandler.run(): Error parsing dates.");
                        break;
                    }
                    startDate = dates[0];
                    endDate = dates[1];

                    // Perform mapping
                    ArrayList<BookingsByLocation> bookingsByLocations = mapper.mapBookingsToLocations(startDate, endDate);

                    // Send to reducer
                    for (BookingsByLocation bbl : bookingsByLocations) {
                        System.out.println("\n> Sending to reducer: " + bbl.getBookingIds());
                    }
                    this.sendMappingToReducer(mapId, null, bookingsByLocations);
                    break;

                default:
                    System.err.println("\n! RequestHandler.run(): Request type not recognized.");
                    break;
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("\n! RequestHandler.run(): Error:\n" + e);
            e.printStackTrace();
        } finally {
            try {
                System.out.println("\n> Closing thread...");
                this.serverSocketIn.close();
                this.serverSocketOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
