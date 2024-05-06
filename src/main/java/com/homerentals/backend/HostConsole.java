package com.homerentals.backend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class HostConsole {
    // TODO: Replace System.out.println() with logger in log file.

    private enum MENU_OPTIONS {
        EXIT("Exit", "0"),
        UPDATE_RENTAL_AVAILABILITY("Update rental availability", "1"),
        VIEW_ALL_BOOKINGS("View all upcoming bookings", "2"),
        VIEW_AMOUNT_OF_BOOKINGS_PER_LOCATION("View amount of bookings per location", "3"),
        UPLOAD_RENTAL_FILE("Upload new rental file", "4"),
        VIEW_RENTALS("View my rentals", "5");

        private final String menuText;
        private final String menuNumber;

        MENU_OPTIONS(String menuText, String menuNumber) {
            this.menuText = menuText;
            this.menuNumber = menuNumber;
        }

        public String getMenuText() {
            return this.menuText;
        }

        public String getMenuNumber() {
            return this.menuNumber;
        }

        public static MENU_OPTIONS getOption(String menuNumber) {
            for (MENU_OPTIONS option : MENU_OPTIONS.values()) {
                if (option.getMenuNumber().equals(menuNumber)) {
                    return option;
                }
            }
            return null;
        }

        public static String getMenuDisplay(int num) {
            String textDisplay = Objects.requireNonNull(getOption(String.valueOf(num))).getMenuText();
            return String.format("%d. %s", num, textDisplay);
        }
    }

    private Socket requestSocket = null;

    private DataOutputStream serverSocketOutput = null;
    private DataInputStream serverSocketInput = null;
    private static final Scanner userInput = new Scanner(System.in);

    public Socket getRequestSocket() {
        return this.requestSocket;
    }

    public void setRequestSocket(Socket requestSocket) throws IOException {
        this.requestSocket = requestSocket;
        try {
            this.serverSocketOutput = new DataOutputStream(this.requestSocket.getOutputStream());
            this.serverSocketInput = new DataInputStream(this.requestSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("\n! HostConsole.setRequestSocket(): Error setting outputs:\n" + e);
            throw e;
        }
    }

    public DataOutputStream getOutputStream() {
        return serverSocketOutput;
    }

    public DataInputStream getInputStream() {
        return this.serverSocketInput;
    }

    private String connectUser() {
        // TODO: Add DAO lookup for user

        String username;
        System.out.print("\tWelcome back!\n" +
                "Enter username\n> ");
        do {
            username = userInput.nextLine().trim();
            if (!username.equals("admin")) {
                System.out.print("User not found! Try again.\n> ");
            }
        } while (!username.equals("admin"));

        String input;
        System.out.print("Enter password\n> ");
        do {
            input = userInput.nextLine().trim();
            if (!input.equals("admin")) {
                System.out.print("Incorrect password! Try again.\n> ");
            }
        } while (!input.equals("admin"));

        return username;
    }

    private void close() throws IOException {
        try {
            JSONObject request = BackendUtils.createRequest(Requests.CLOSE_CONNECTION.name(), "");
            BackendUtils.clientToServer(serverSocketOutput, request.toString());
            this.serverSocketInput.close();
            this.serverSocketOutput.close();
            this.requestSocket.close();
        } catch (IOException e) {
            System.err.println("\n! HostConsole.close(): Error closing sockets:\n" + e);
            throw e;
        }
    }

    public static void main(String[] args) {
        HostConsole hostConsole = new HostConsole();

        String username = hostConsole.connectUser();

        try {
            // Establish a connection
            Socket requestSocket = null;
            System.out.println("Connecting to server...");
            requestSocket = new Socket(BackendUtils.SERVER_ADDRESS, BackendUtils.SERVER_PORT);
            hostConsole.setRequestSocket(requestSocket);
            DataOutputStream outputStream = hostConsole.getOutputStream();
            DataInputStream inputStream = hostConsole.getInputStream();

            JSONObject request, requestBody, responseJson, responseBody;
            ArrayList<JSONObject> rentals;
            String response;
            boolean done = false;
            while (!done) {
                System.out.println("\n\n\t[MENU]");
                for (int i = 1; i < MENU_OPTIONS.values().length; i++) {
                    System.out.println(MENU_OPTIONS.getMenuDisplay(i));
                }
                System.out.println(MENU_OPTIONS.getMenuDisplay(0));
                System.out.print("> ");

                MENU_OPTIONS option = MENU_OPTIONS.getOption(userInput.nextLine().trim());
                if (option == null) {
                    continue;
                }

                switch (option) {
                    case EXIT:
                        done = true;
                        break;

                    case UPLOAD_RENTAL_FILE:
                        System.out.print("Enter name of the directory containing the rental information.\n> ");
                        String directory = userInput.nextLine().trim();
                        String filePath = BackendUtils.inputsPath + directory + "/" + directory;

                        // Read JSON file
                        JSONObject newRental = BackendUtils.readFile(filePath, false);
                        if (newRental == null) {
                            System.err.println("\n! HostConsole.main(): Error reading JSON File.");
                            break;
                        }

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.NEW_RENTAL.name(), newRental.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());
                        break;

                    case UPDATE_RENTAL_AVAILABILITY:
                        // Print rentals list
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, username);
                        if (rentals == null) {
                            System.err.println("\n! HostConsole.main(): Error getting Rentals list.");
                            break;
                        }

                        int rentalId = BackendUtils.chooseRentalFromList(rentals);
                        // Get start and end days to mark available
                        requestBody = BackendUtils.getInputDatesAsJsonObject("mark available");
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentalId);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        BackendUtils.handleServerResponse(inputStream, "Change successful!", "Change unsuccessful.\nCheck rental bookings and try again.");
                        break;

                    case VIEW_ALL_BOOKINGS:
                        // Create and send request
                        // Filters is needed for MapReduce within the server.
                        // It is empty so we can get all rentals.
                        JSONObject filters = new JSONObject();
                        JSONObject body = new JSONObject();
                        body.put(BackendUtils.BODY_FIELD_FILTERS, filters);
                        request = BackendUtils.createRequest(Requests.GET_ALL_BOOKINGS.name(), body.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        response = BackendUtils.serverToClient(inputStream);
                        if (response == null) {
                            System.err.println("\n! HostConsole.main(): Could not receive response from Server.");
                            break;
                        }

                        // Handle JSONArray of bookings per rental
                        responseJson = new JSONObject(response);
                        responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
                        JSONArray rentalsWithBookings = responseBody.getJSONArray(BackendUtils.BODY_FIELD_RENTALS_WITH_BOOKINGS);
                        JSONObject rentalInfoAndBookings;
                        JSONArray bookingInfoOfThisRental;
                        for (int i = 0; i < rentalsWithBookings.length(); i++) {
                            rentalInfoAndBookings = rentalsWithBookings.getJSONObject(i);
                            System.out.printf("%n[Rental: %s]%n%n", rentalInfoAndBookings.get(BackendUtils.BODY_FIELD_RENTAL_STRING));

                            bookingInfoOfThisRental = rentalInfoAndBookings.getJSONArray(BackendUtils.BODY_FIELD_BOOKINGS);
                            for (int j = 0; j < bookingInfoOfThisRental.length(); j++) {
                                JSONObject bookingString = bookingInfoOfThisRental.getJSONObject(j);
                                System.out.printf("- %s%n", bookingString.get(BackendUtils.BODY_FIELD_BOOKING_STRING));
                            }
                        }
                        break;

                    case VIEW_AMOUNT_OF_BOOKINGS_PER_LOCATION:
                        // Get start and end days to show bookings
                        requestBody = BackendUtils.getInputDatesAsJsonObject("view bookings");

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS_BY_LOCATION.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        response = BackendUtils.serverToClient(inputStream);
                        if (response == null) {
                            System.err.println("\n! HostConsole.main(): Could not receive host's rentals from Server.");
                            break;
                        }

                        // Handle JSONArray of bookings by location
                        responseJson = new JSONObject(response);
                        responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
                        JSONArray bookingsByLocation = responseBody.getJSONArray(BackendUtils.BODY_FIELD_BOOKINGS_BY_LOCATION);
                        System.out.printf("%n[%s's Bookings By Location]%n%n", username);
                        for (int i = 0; i < bookingsByLocation.length(); i++) {
                            JSONObject byLocation = bookingsByLocation.getJSONObject(i);
                            System.out.print(byLocation.get(BackendUtils.BODY_FIELD_BY_LOCATION));
                        }
                        System.out.println("<-------- [End Of List] -------->");
                        break;

                    case VIEW_RENTALS:
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, username);
                        if (rentals == null) {
                            System.err.println("\n! HostConsole.main(): Error getting Rentals list.");
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException | JSONException e) {
            System.err.println("\n! HostConsole.main(): Error:\n" + e);
            e.printStackTrace();
        } finally {
            if (hostConsole.getRequestSocket() != null) {
                try {
                    System.out.println("Closing down connection...");
                    hostConsole.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
