package com.homerentals.backend;

import com.homerentals.domain.Filters;
import com.homerentals.domain.Rental;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.ArrayList;

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

            JSONObject request;
            switch (inputHeader) {
                // Guest Requests
                case GET_RENTALS:
                    // Parse JSON Message
                    int mapId = inputBody.getInt(BackendUtils.BODY_FIELD_MAP_ID);
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

                    // Perform mapping Operation
                    MapSearch mapper = new MapSearch(filters, Worker.rentals);
                    ArrayList<Rental> mappedRentals = mapper.map();

                    // Wrap results in object
                    MapResult mapResult = new MapResult(mapId, mappedRentals);

                    // Send results to reducer
                    try {
                        Worker.writeToReducerSocket(mapResult);
                    } catch (IOException e) {
                        System.err.println("RequestHandler.run(): Error writing to Reducer Socket: " + e);
                    }
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
                    Rental rental = BackendUtils.jsonToRentalObject(inputBody);
                    if (rental == null) {
                        System.err.println("RequestHandler.run(): Error creating Rental object from JSON");
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