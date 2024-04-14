package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class BackendUtils {
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    public static final String MESSAGE_TYPE = "type";
    public static final String MESSAGE_HEADER = "header";
    public static final String MESSAGE_BODY = "body";

    public static final String MESSAGE_TYPE_REQUEST = "request";
    public static final String MESSAGE_TYPE_RESPONSE = "response";

    public static final String BODY_FIELD_FILTERS = "filters";
    public static final String BODY_FIELD_MAP_ID = "mapId";
    public static final String BODY_FIELD_FOR_RENTALS = "forRentals";
    public static final String BODY_FIELD_RENTAL_ID = "rentalId";
    public static final String BODY_FIELD_RENTAL_LIST = "rentalList";
    public static final String BODY_FIELD_START_DATE = "startDate";
    public static final String BODY_FIELD_END_DATE = "endDate";
    public static final String BODY_FIELD_BOOKING_ID = "bookingId";
    public static final String BODY_FIELD_GUEST_EMAIL = "guestEmail";
    public static final String BODY_FIELD_GUEST_PASSWORD = "guestPassword";
    public static final String BODY_FIELD_RATING = "rating";

    public static final int SERVER_PORT = 8080;
    public static final int REDUCER_PORT = 4040;

    public static JSONObject createRequest(String header, String body) {
        JSONObject request = new JSONObject();
        request.put(MESSAGE_TYPE, MESSAGE_TYPE_REQUEST);
        request.put(MESSAGE_HEADER, header);
        if (body.isEmpty()) body = "{}";
        request.put(MESSAGE_BODY, body);
        return request;
    }

    public static JSONObject readFile(String path) {
        // Read JSON file
        try {
            InputStream is = Files.newInputStream(Paths.get(path));
            String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
            System.out.println(jsonTxt);
            return new JSONObject(jsonTxt);
        } catch (IOException | JSONException e) {
            // Could not find file or
            // File is not valid JSON Object
            System.err.println("Client.readFile(): Error reading JSON File: " + e.getMessage());
            return null;
        }
    }

    public static Rental jsonToRentalObject(JSONObject input) {
        try {
            // Create Rental object from JSON
            String roomName = input.getString("roomName");
            String location = input.getString("location");
            double pricePerNight = input.getDouble("nightlyRate");
            int numOfPersons = input.getInt("capacity");
            int numOfRatings = input.getInt("numOfRatings");
            int sumOfRatings = input.getInt("sumOfRatings");
            String imagePath = input.getString("imagePath");
            int rentalId = input.getInt("rentalId");
            Rental rental = new Rental(null, roomName, location, pricePerNight,
                    numOfPersons, numOfRatings, sumOfRatings, imagePath, rentalId);
            System.out.println(rental.getId());
            return rental;

        } catch (JSONException e) {
            // String is not valid JSON object
            System.err.println("RequestHandler.jsonToRentalObject(): Error creating Rental object from JSON: " + e);
            return null;
        }
    }

    /**
     * @return JSONObject : {"startDate", "endDate"}
     */
    protected static JSONObject getInputDatesAsJsonObject(String msg) {
        Scanner userInput = new Scanner(System.in);
        JSONObject result = new JSONObject();
        String input = "";

        System.out.printf("Enter start date to %s\n" +
                "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
        boolean invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, BackendUtils.dateFormatter);
                invalid = false;
            } catch (DateTimeParseException e) {
                System.out.print("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put(BackendUtils.BODY_FIELD_START_DATE, input);

        System.out.printf("Enter end date to %s\n" +
                "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
        invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, BackendUtils.dateFormatter);
                invalid = false;

            } catch (DateTimeParseException e) {
                System.out.print("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put(BackendUtils.BODY_FIELD_END_DATE, input);

        return result;
    }

    /*
    Used in ClientHandler for NEW_RENTAL request
    and Server.setUp()
     */
    protected static void executeNewRentalRequest(JSONObject body, String header) {
        // Add new rentalId to requestBody
        int rentalId = Server.getNextRentalId();
        body.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentalId);
        JSONObject request = BackendUtils.createRequest(header, body.toString());

        // Forward new request to worker that will contain this rental
        int workerPort = Server.ports.get(Server.hash(rentalId));
        Server.sendMessageToWorker(request.toString(), workerPort);
    }

    /*
    Used in ClientHandler for UPDATE_AVAILABILITY request
    and Server.setUp()
     */
    public static void executeUpdateAvailability(String input, JSONObject body) {
        // Forward request, as it is,
        // to worker that contains this rental
        int workerPort = Server.ports.get(Server.hash(body.getInt(BackendUtils.BODY_FIELD_RENTAL_ID)));
        Server.sendMessageToWorker(input, workerPort);
    }

    /*
    Used in ClientHandler for NEW_BOOKING request
    and Server.setUp()
     */
    protected static void executeNewBookingRequest(JSONObject body, String header) {
        // Add new bookingId to requestBody
        String bookingId = Server.getNextBookingId();
        body.put(BackendUtils.BODY_FIELD_BOOKING_ID, bookingId);
        JSONObject request = BackendUtils.createRequest(header, body.toString());

        // Forward new request to worker that contains this rental
        int rentalId = body.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
        int workerPort = Server.ports.get(Server.hash(rentalId));
        Server.sendMessageToWorker(request.toString(), workerPort);
    }

    protected static void clientToServer(DataOutputStream stream, String msg) throws IOException {
        try {
            stream.writeUTF(msg);
            stream.flush();
        } catch (IOException e) {
            System.err.println("BackendUtils.clientToServer(): Error sending Socket Output: " + e.getMessage());
            throw e;
        }
    }

    protected static Object serverToClient(ObjectInputStream stream) {
        try {
            return stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("GuestConsole.readSocketObjectInput(): Could not read object from server input stream: " + e.getMessage());
            return null;
        }
    }

    /*
    Executes MapReduce request for all rentals
    from HostConsole / GuestConsole
     */
    protected static ArrayList<Rental> getAllRentals(DataOutputStream dataOutputStream, ObjectInputStream objectInputStream, String username) throws IOException {
        // Create and send request
        JSONObject filters = new JSONObject();
        JSONObject body = new JSONObject();
        body.put(BackendUtils.BODY_FIELD_FILTERS, filters);
        JSONObject request = BackendUtils.createRequest(Requests.GET_RENTALS.name(), body.toString());
        try {
            BackendUtils.clientToServer(dataOutputStream, request.toString());
        } catch (IOException e) {
            System.err.println("BackendUtils.getAllRentals(): Error sending Socket Output: " + e.getMessage());
            throw e;
        }

        // Receive response
        ArrayList<Rental> rentals = (ArrayList<Rental>) BackendUtils.serverToClient(objectInputStream);
        if (rentals == null) {
            System.err.println("BackendUtils.getAllRentals(): Could not receive host's rentals from Server.");
            return null;
        }

        if (username != null) {
            System.out.printf("%n[%s's Rentals List]%n%n", username);
        } else {
            System.out.println("\n[Rentals List]\n");
        }
        for (int i = 0; i < rentals.size(); i++) {
            System.out.printf("[%d] %s%n", i, rentals.get(i));
        }
        System.out.println("<-------- [End Of List] -------->");
        return rentals;
    }

    protected static Rental chooseRentalFromList(ArrayList<Rental> rentals) {
        Scanner userInput = new Scanner(System.in);

        System.out.print("\nChoose rental\n> ");
        int rentalIndex = -1;
        do {
            try {
                rentalIndex = Integer.parseInt(userInput.nextLine().trim());
                if (rentalIndex < 0 || rentalIndex >= rentals.size()) {
                    System.out.print("Invalid input. Try again\n> ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Try again\n> ");
            }
        } while (rentalIndex < 0 || rentalIndex >= rentals.size());

        return rentals.get(rentalIndex);
    }

    // TODO: write to worker/reducer socket

    // TODO: read client/worker socket
}
