package com.homerentals.backend;

import com.homerentals.domain.Filters;
import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

class RequestHandler implements Runnable {
    private final Socket masterSocket;
    private DataInputStream masterSocketIn = null;

    protected RequestHandler(Socket masterSocket) throws IOException {
        this.masterSocket = masterSocket;
        try {
            this.masterSocketIn = new DataInputStream(this.masterSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("RequestHandler(): Error setting up stream: " + e);
            throw e;
        }
    }

    private String readMasterSocketInput() {
        try {
            return this.masterSocketIn.readUTF();
        } catch (IOException e) {
            System.err.println("RequestHandler.readMasterSocketInput(): Error reading Client Socket input: " + e);
            return null;
        }
    }

    private LocalDate[] parseJsonDates(JSONObject json) {
        String startDateString = json.getString(BackendUtils.BODY_FIELD_START_DATE);
        String endDateString = json.getString(BackendUtils.BODY_FIELD_END_DATE);
        LocalDate startDate = LocalDate.parse(startDateString, BackendUtils.dateFormatter);
        LocalDate endDate = LocalDate.parse(endDateString, BackendUtils.dateFormatter);
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
            input = this.readMasterSocketInput();
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
            int mapId;
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

                    // Iterate over each filter in Filters enum
                    for (Filters f : Filters.values()) {
                        String filterName = f.name();
                        String filterValue = "";
                        if (jsonFilters.has(filterName)) {
                            filterValue = jsonFilters.getString(filterName);
                        }
                        filters.put(filterName, filterValue);
                        System.out.printf("Storing filter: [%s = %s]%n", filterName, filterValue);
                    }

                    System.out.println("Created filters map: " + filters);

                    // Perform mapping
                    ArrayList<Rental> mappedRentals = mapper.mapRentalsToFilters(filters);

                    // Send to reducer
                    this.sendMappingToReducer(mapId, mappedRentals, null);
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
                    int rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                    rental = Worker.idToRental.get(rentalId);

                    // Get LocalDate objects
                    dates = parseJsonDates(inputBody);
                    startDate = dates[0];
                    endDate = dates[1];
                    synchronized (rental) {
                        System.out.println("lock");
                        System.out.println(rental.getAvailability(startDate, endDate));
                        rental.makeAvailable(startDate, endDate);
                    }
                    System.out.println("done");
                    System.out.println(rental.getAvailability(startDate, endDate));
                    break;

                case GET_BOOKINGS:
                    // Parse JSON Message
                    mapId = inputBody.getInt(BackendUtils.BODY_FIELD_MAP_ID);
                    dates = parseJsonDates(inputBody);
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
