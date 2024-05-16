package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class BackendUtils {
    public static final String inputsPath = "src/main/java/com/homerentals/inputs/";
    public static final String filtersPath = "src/main/java/com/homerentals/inputs/filters/";

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);

    public static final String MESSAGE_TYPE = "type";
    public static final String MESSAGE_HEADER = "header";
    public static final String MESSAGE_BODY = "body";

    public static final String MESSAGE_TYPE_REQUEST = "request";
    public static final String MESSAGE_TYPE_RESPONSE = "response";

    public static final String BODY_FIELD_FILTERS = "filters";
    public static final String BODY_FIELD_MAP_ID = "mapId";
    public static final String BODY_FIELD_RENTAL_ID = "rentalId";
    public static final String BODY_FIELD_START_DATE = "startDate";
    public static final String BODY_FIELD_END_DATE = "endDate";
    public static final String BODY_FIELD_BOOKING_ID = "bookingId";
    public static final String BODY_FIELD_GUEST_EMAIL = "guestEmail";
    public static final String BODY_FIELD_GUEST_PASSWORD = "guestPassword";
    public static final String BODY_FIELD_RENTAL_NAME = "rentalName";
    public static final String BODY_FIELD_RENTAL_LOCATION = "rentalLocation";
    public static final String BODY_FIELD_RATING = "rating";

    public static final String BODY_FIELD_STATUS = "status";
    public static final String BODY_FIELD_RENTALS = "rentals";
    public static final String BODY_FIELD_RENTAL_STRING = "rentalString";
    public static final String BODY_FIELD_BOOKINGS = "bookings";
    public static final String BODY_FIELD_BOOKING_STRING = "bookingString";
    public static final String BODY_FIELD_RENTALS_WITH_BOOKINGS = "rentalsWithBookings";
    public static final String BODY_FIELD_BOOKINGS_BY_LOCATION = "bookingsByLocation";
    public static final String BODY_FIELD_BY_LOCATION = "byLocation";

    // TODO use these via a config file
    public static final String SERVER_ADDRESS = "localhost";
    public static final String REDUCER_ADDRESS = "localhost";

    public static final int SERVER_PORT = 8080;
    public static final int REDUCER_PORT = 4040;


    /*
    Creates new request
     */
    public static JSONObject createRequest(String header, String body) {
        JSONObject request = new JSONObject();
        request.put(MESSAGE_TYPE, MESSAGE_TYPE_REQUEST);
        request.put(MESSAGE_HEADER, header);
        if (body.isEmpty()) body = "{}";
        request.put(MESSAGE_BODY, body);
        return request;
    }

    public static JSONObject createResponse(String header, String body) {
        JSONObject response = new JSONObject();
        response.put(MESSAGE_TYPE, MESSAGE_TYPE_RESPONSE);
        response.put(MESSAGE_HEADER, header);
        if (body.isEmpty()) body = "{}";
        response.put(MESSAGE_BODY, body);
        return response;
    }

    public static JSONObject readFile(String path, boolean print) {
        path += ".json";
        // Read JSON file
        try {
            InputStream is = Files.newInputStream(Paths.get(path));
            String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
            if (print)
                System.out.printf("%n> BackendUtils.readFile(%s):%n%s%n%n", path, jsonTxt);
            return new JSONObject(jsonTxt);
        } catch (IOException | JSONException e) {
            // Could not find file or
            // File is not valid JSON Object
            System.err.println("\n! BackendUtils.readFile(): Error reading JSON File:\n" + e);
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
            int rentalId = input.getInt("rentalId");
            return new Rental(null, roomName, location, pricePerNight,
                    numOfPersons, numOfRatings, sumOfRatings, rentalId);
        } catch (JSONException e) {
            // String is not valid JSON object
            System.err.println("\n! RequestHandler.jsonToRentalObject(): Error creating Rental object from JSON:\n" + e);
            return null;
        }
    }

    /**
     * Prints prompts for start and end date inputs.
     * Also checks if the time period is valid
     * (the start date is before the end date).
     *
     * @return JSONObject : {"startDate", "endDate"}
     */
    protected static JSONObject getInputDatesAsJsonObject(String msg) {
        Scanner userInput = new Scanner(System.in);
        LocalDate startDate = null;
        LocalDate endDate = null;
        JSONObject result = new JSONObject();
        String input = "";
        boolean validTimePeriod = false;
        while (!validTimePeriod) {
            System.out.printf("Enter start date to %s\n" +
                    "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
            boolean invalidDateInput = true;
            while (invalidDateInput) {
                try {
                    input = userInput.nextLine().trim();
                    startDate = LocalDate.parse(input, dateFormatter);
                    invalidDateInput = false;
                } catch (DateTimeParseException e) {
                    System.out.print("Invalid input! Try again.\n> ");
                }
            }
            result.put(BODY_FIELD_START_DATE, input);

            System.out.printf("Enter end date to %s\n" +
                    "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
            invalidDateInput = true;
            while (invalidDateInput) {
                try {
                    input = userInput.nextLine().trim();
                    endDate = LocalDate.parse(input, dateFormatter);
                    invalidDateInput = false;

                } catch (DateTimeParseException e) {
                    System.out.print("Invalid input! Try again.\n> ");
                }
            }

            if (startDate.isBefore(endDate)) {
                validTimePeriod = true;
                result.put(BODY_FIELD_END_DATE, input);
            } else {
                System.out.print("Invalid dates! Try again.\n> ");
            }
        }

        return result;
    }

    /*
    Used in ClientHandler for NEW_RENTAL request
    and Server.setUp()
     */
    protected static void executeNewRentalRequest(JSONObject body, String header) {
        // Add new rentalId to requestBody
        int rentalId;
        synchronized (Server.rentalIdSyncObj) {
            rentalId = Server.getNextRentalId();
        }
        body.put(BODY_FIELD_RENTAL_ID, rentalId);
        JSONObject request = createRequest(header, body.toString());

        // Forward new request to worker that will contain this rental
        int workerId = Server.hash(rentalId);
        Server.sendMessageToWorker(request.toString(), workerId);
    }

    /*
    Used in ClientHandler for UPDATE_AVAILABILITY request
    and Server.setUp()
     */
    public static String executeUpdateAvailability(String input, JSONObject body) {
        // Forward request, as it is,
        // to worker that contains this rental
        int workerId = Server.hash(body.getInt(BODY_FIELD_RENTAL_ID));
        return Server.sendMessageToWorkerAndWaitForResponse(input, workerId);
    }

    /**
     * Used in ClientHandler for NEW_BOOKING request
     * and Server.setUp().
     * If the booking was successful, it adds it to the guest's list.
     *
     * @return if the communication with the worker was unsuccessful, returns null.
     * else, returns the JSONObject of the response body
     */
    protected static JSONObject executeNewBookingRequest(JSONObject body, String header) {
        // Add new bookingId to requestBody
        String bookingId;
        synchronized (Server.bookingIdSyncObj) {
            bookingId = Server.getNextBookingId();
        }
        body.put(BODY_FIELD_BOOKING_ID, bookingId);
        JSONObject request = createRequest(header, body.toString());

        // Forward new request to worker that contains this rental
        int rentalId = body.getInt(BODY_FIELD_RENTAL_ID);
        int workerId = Server.hash(rentalId);
        String response = Server.sendMessageToWorkerAndWaitForResponse(request.toString(), workerId);
        if (response == null) {
            return null;
        }

        // Handle JSON response
        JSONObject responseJson = new JSONObject(response);
        JSONObject responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
        String status = responseBody.getString(BackendUtils.BODY_FIELD_STATUS);
        if (status.equals("OK")) {
            String email = responseBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
            bookingId = responseBody.getString(BackendUtils.BODY_FIELD_BOOKING_ID);
            rentalId = responseBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
            String rentalName = responseBody.getString(BackendUtils.BODY_FIELD_RENTAL_NAME);
            String rentalLocation = responseBody.getString(BackendUtils.BODY_FIELD_RENTAL_LOCATION);
            String startDateString = responseBody.getString(BackendUtils.BODY_FIELD_START_DATE);
            String endDateString = responseBody.getString(BackendUtils.BODY_FIELD_END_DATE);
            LocalDate startDate = LocalDate.parse(startDateString, BackendUtils.dateFormatter);
            LocalDate endDate = LocalDate.parse(endDateString, BackendUtils.dateFormatter);

            Server.addBookingToGuest(email, bookingId, rentalId, rentalName, rentalLocation, startDate, endDate);
        }
        return responseBody;
    }

    /*
    Used by HostConsole and GuestConsole clients
    to send requests to Server
     */
    protected static void clientToServer(DataOutputStream stream, String msg) throws IOException {
        try {
            stream.writeUTF(msg);
            stream.flush();
        } catch (IOException e) {
            System.err.println("\n! BackendUtils.clientToServer(): Error sending Socket Output:\n" + e);
            throw e;
        }
    }

    /*
    Used by HostConsole and GuestConsole clients
    to receive responses from Server
     */
    protected static String serverToClient(DataInputStream stream) {
        try {
            return stream.readUTF();
        } catch (IOException e) {
            System.err.println("\n! BackendUtils.serverToClient(): Could not read object from server input stream:\n" + e);
            return null;
        }
    }

    protected static void handleServerResponse(DataInputStream stream, String successfulMsg, String unsuccessfulMsg) throws IOException {
        // Receive responseString
        String responseString = BackendUtils.serverToClient(stream);
        if (responseString == null) {
            System.err.println("\n! BackendUtils.handleServerResponse(): Could not receive responseString from Server.");
            return;
        }
        // Handle JSON input
        JSONObject responseJson = new JSONObject(responseString);
        JSONObject responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
        String status = responseBody.getString(BackendUtils.BODY_FIELD_STATUS);
        if (status.equals("OK")) {
            System.out.println(successfulMsg);
        } else {
            System.out.println(unsuccessfulMsg);
        }
    }

    /*
    Executes MapReduce request for all rentals
    from HostConsole / GuestConsole
     */
    protected static ArrayList<JSONObject> getAllRentals(DataOutputStream dataOutputStream, DataInputStream objectInputStream, String username) throws IOException {
        // Create and send request
        JSONObject filters = new JSONObject();
        JSONObject body = new JSONObject();
        body.put(BODY_FIELD_FILTERS, filters);
        JSONObject request = createRequest(Requests.GET_RENTALS.name(), body.toString());
        try {
            clientToServer(dataOutputStream, request.toString());
        } catch (IOException e) {
            System.err.println("\n! BackendUtils.getAllRentals(): Error sending Socket Output:\n" + e);
            throw e;
        }

        // Receive response
        String response = serverToClient(objectInputStream);
        if (response == null) {
            System.err.println("\n! BackendUtils.getAllRentals(): Could not receive host's rentals from Server.");
            return null;
        }

        // Handle JSONArray of rentals
        if (username != null) {
            System.out.printf("%n[%s's Rentals List]%n%n", username);
        } else {
            System.out.println("\n[Rentals List]\n");
        }
        JSONObject responseJson = new JSONObject(response);
        JSONObject responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
        JSONArray rentalsJsonArray = responseBody.getJSONArray(BODY_FIELD_RENTALS);
        ArrayList<JSONObject> rentals = new ArrayList<>();
        for (int i = 0; i < rentalsJsonArray.length(); i++) {
            JSONObject rental = rentalsJsonArray.getJSONObject(i);
            System.out.printf("[%d] %s%n", i, rental.get(BODY_FIELD_RENTAL_STRING));
            rentals.add(rental);
        }
        System.out.println("<-------- [End Of List] -------->");

        return rentals;
    }

    protected static int chooseRentalFromList(ArrayList<JSONObject> rentals) {
        Scanner userInput = new Scanner(System.in);

        System.out.print("\nChoose rental\n> ");
        int rentalIndex = -1;
        do {
            try {
                rentalIndex = Integer.parseInt(userInput.nextLine().trim());
                if (rentalIndex < 0 || rentalIndex >= rentals.size()) {
                    System.out.print("Invalid input! Try again.\n> ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Try again.\n> ");
            }
        } while (rentalIndex < 0 || rentalIndex >= rentals.size());

        return rentals.get(rentalIndex).getInt(BODY_FIELD_RENTAL_ID);
    }
}
