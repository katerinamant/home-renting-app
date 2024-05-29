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

public class GuestConsole {
    // TODO: Replace System.out.println() with logger in log file.

    private enum MENU_OPTIONS {
        EXIT("Exit", "0"),
        UPLOAD_FILTERS_FILE("Upload new filters file", "1"),
        VIEW_ALL_RENTALS("View all rentals", "2"),
        RATE_STAY("Rate your stay", "3");

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

        public static GuestConsole.MENU_OPTIONS getOption(String menuNumber) {
            for (GuestConsole.MENU_OPTIONS option : GuestConsole.MENU_OPTIONS.values()) {
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
            System.err.println("\n! GuestConsole.setRequestSocket(): Error setting outputs:\n" + e);
            throw e;
        }
    }

    public DataOutputStream getOutputStream() {
        return this.serverSocketOutput;
    }

    public DataInputStream getInputStream() {
        return this.serverSocketInput;
    }

    private String[] connectUser() {
        // Establish a connection
        Socket requestSocket;
        System.out.println("Connecting to server...");
        try {
            requestSocket = new Socket(BackendUtils.SERVER_ADDRESS, BackendUtils.SERVER_PORT);
            this.setRequestSocket(requestSocket);
        } catch (IOException e) {
            System.err.println("\n! GuestConsole.connectUser(): Error setting up socket:\n" + e);
            return null;
        }

        System.out.println("\tWelcome back!");
        String email, password;
        while (true) {
            JSONObject requestBody = new JSONObject();

            System.out.print("Enter email\n> ");
            email = userInput.nextLine().trim();
            requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);
            System.out.print("Enter password\n> ");
            password = userInput.nextLine().trim();
            requestBody.put(BackendUtils.BODY_FIELD_GUEST_PASSWORD, password);

            // Write to socket
            System.out.println("Writing to server...");
            JSONObject request = BackendUtils.createRequest(Requests.CHECK_CREDENTIALS.name(), requestBody.toString());
            try {
                BackendUtils.clientToServer(this.serverSocketOutput, request.toString());
            } catch (IOException e) {
                System.err.println("\n! GuestConsole.connectUser(): Error sending to server:\n" + e);
                return null;
            }

            // Receive responseString
            String responseString = BackendUtils.serverToClient(this.serverSocketInput);
            if (responseString == null) {
                System.err.println("\n! GuestConsole.connectUser(): Could not receive responseString from Server.");
                return null;
            }
            // Handle JSON input
            JSONObject responseJson = new JSONObject(responseString);
            JSONObject responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
            String status = responseBody.getString(BackendUtils.BODY_FIELD_STATUS);
            if (status.equals("OK")) {
                break;
            } else {
                System.out.println("Invalid credentials! Try again.");
            }
        }

        return new String[]{email, password};
    }

    private void printRentalsList(ArrayList<JSONObject> rentals) {
        System.out.println("\n[Rentals List]\n");
        for (int i = 0; i < rentals.size(); i++) {
            System.out.printf("[%d] %s%n", i, rentals.get(i).get(BackendUtils.BODY_FIELD_RENTAL_STRING));
        }
        System.out.println("<-------- [End Of List] -------->");
    }

    private void bookNewRental(ArrayList<JSONObject> rentals, String email) throws IOException {
        // Ask user for new booking
        System.out.print("\n\nWould you like to book a rental? (Y/N)\n> ");
        String ans;
        do {
            ans = userInput.nextLine().trim();
            if (!ans.equalsIgnoreCase("Y") && !ans.equalsIgnoreCase("N")) {
                System.out.print("Invalid input! Try again.\n> ");
            }
        } while (!ans.equalsIgnoreCase("Y") && !ans.equalsIgnoreCase("N"));

        if (ans.equalsIgnoreCase("N")) {
            return;
        }

        this.printRentalsList(rentals);
        int rentalId = BackendUtils.chooseRentalFromList(rentals);
        // Get start and end days of booking
        JSONObject requestBody = BackendUtils.getInputDatesAsJsonObject("book rental");
        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentalId);
        requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);

        JSONObject request = BackendUtils.createRequest(Requests.NEW_BOOKING.name(), requestBody.toString());
        BackendUtils.clientToServer(this.serverSocketOutput, request.toString());

        BackendUtils.handleServerResponse(this.serverSocketInput, "Booking successful!", "Booking failed. Try again another time.");
    }

    private void close() throws IOException {
        try {
            JSONObject request = BackendUtils.createRequest(Requests.CLOSE_CONNECTION.name(), "");
            BackendUtils.clientToServer(this.serverSocketOutput, request.toString());
            this.serverSocketInput.close();
            this.serverSocketOutput.close();
            this.requestSocket.close();
        } catch (IOException e) {
            System.err.println("\n! GuestConsole.close(): Error closing sockets:\n" + e);
            throw e;
        }
    }

    public static void main(String[] args) {
        GuestConsole guestConsole = new GuestConsole();

        try {
            String[] userInfo = guestConsole.connectUser();
            if (userInfo == null) {
                System.err.println("\n! GuestConsole.main(): Error connecting user.");
                throw new IOException();
            }
            String email = userInfo[0];

            DataOutputStream outputStream = guestConsole.getOutputStream();
            DataInputStream inputStream = guestConsole.getInputStream();

            JSONObject request, requestBody, responseJson, responseBody;
            ArrayList<JSONObject> rentals;
            String response;
            boolean done = false;
            while (!done) {
                System.out.println("\n\n\t[MENU]");
                for (int i = 1; i < GuestConsole.MENU_OPTIONS.values().length; i++) {
                    System.out.println(GuestConsole.MENU_OPTIONS.getMenuDisplay(i));
                }
                System.out.println(GuestConsole.MENU_OPTIONS.getMenuDisplay(0));
                System.out.print("> ");

                GuestConsole.MENU_OPTIONS option = GuestConsole.MENU_OPTIONS.getOption(userInput.nextLine().trim());
                if (option == null) {
                    continue;
                }

                switch (option) {
                    case EXIT:
                        done = true;
                        break;

                    case UPLOAD_FILTERS_FILE:
                        System.out.print("Enter name of .json file that contains the filters.\n> ");
                        String filePath = BackendUtils.filtersPath + userInput.nextLine().trim();

                        // Read JSON file
                        JSONObject filters = BackendUtils.readFile(filePath, false);
                        if (filters == null) {
                            System.err.println("\n! GuestConsole.main(): Error reading JSON File.");
                            break;
                        }
                        requestBody = new JSONObject();
                        requestBody.put(BackendUtils.BODY_FIELD_FILTERS, filters);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_RENTALS.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        response = BackendUtils.serverToClient(inputStream);
                        if (response == null) {
                            System.err.println("\n! GuestConsole.main(): Could not receive rentals from Server.");
                            break;
                        }

                        // Handle JSONArray of rentals
                        responseJson = new JSONObject(response);
                        responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
                        JSONArray rentalsJsonArray = responseBody.getJSONArray(BackendUtils.BODY_FIELD_RENTALS);
                        rentals = new ArrayList<>();
                        for (int i = 0; i < rentalsJsonArray.length(); i++) {
                            JSONObject rental = rentalsJsonArray.getJSONObject(i);
                            rentals.add(rental);
                        }
                        guestConsole.printRentalsList(rentals);

                        try {
                            guestConsole.bookNewRental(rentals, email);
                        } catch (IOException e) {
                            System.err.println("\n! GuestConsole.main(): Error booking rental:\n" + e);
                        }
                        break;

                    case VIEW_ALL_RENTALS:
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, null);
                        if (rentals == null) {
                            System.err.println("\n! GuestConsole.main(): Error getting Rentals list.");
                            break;
                        }

                        try {
                            guestConsole.bookNewRental(rentals, email);
                        } catch (IOException e) {
                            System.err.println("\n! GuestConsole.main(): Error booking rental: " + e);
                        }
                        break;

                    case RATE_STAY:
                        // Get all booking with no ratings
                        requestBody = new JSONObject();
                        requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS_WITH_NO_RATINGS.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        response = BackendUtils.serverToClient(inputStream);
                        if (response == null) {
                            System.err.println("\n! GuestConsole.main(): Could not receive bookings from Server.");
                            break;
                        }

                        // Handle JSONArray of bookings
                        responseJson = new JSONObject(response);
                        responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
                        JSONArray bookings = responseBody.getJSONArray(BackendUtils.BODY_FIELD_BOOKINGS);
                        System.out.println("\n[Previous Stays]\n");
                        for (int i = 0; i < bookings.length(); i++) {
                            JSONObject booking = bookings.getJSONObject(i);
                            String rentalName = booking.getString(BackendUtils.BODY_FIELD_RENTAL_NAME);
                            String rentalLocation = booking.getString(BackendUtils.BODY_FIELD_RENTAL_LOCATION);
                            String bookingDates = booking.getString(BackendUtils.BODY_FIELD_BOOKING_DATES_STRING);
                            System.out.printf("[%d] Your stay at %s in %s %s%n", i, rentalName, rentalLocation, bookingDates);
                        }
                        System.out.println("<-------- [End Of List] -------->");

                        if (bookings.isEmpty()) {
                            break;
                        }

                        // Create NEW_RATING request body
                        requestBody = new JSONObject();
                        requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);

                        System.out.print("\nChoose stay to rate\n> ");
                        int bookingIndex = -1;
                        do {
                            try {
                                bookingIndex = Integer.parseInt(userInput.nextLine().trim());
                                if (bookingIndex < 0 || bookingIndex >= bookings.length()) {
                                    System.out.print("Invalid input! Try again.\n> ");
                                }
                            } catch (NumberFormatException e) {
                                System.out.print("Invalid input! Try again.\n> ");
                            }
                        } while (bookingIndex < 0 || bookingIndex >= bookings.length());
                        JSONObject booking = bookings.getJSONObject(bookingIndex);
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, booking.get(BackendUtils.BODY_FIELD_RENTAL_ID));
                        requestBody.put(BackendUtils.BODY_FIELD_BOOKING_ID, booking.get(BackendUtils.BODY_FIELD_BOOKING_ID));

                        System.out.print("\nRate your stay [1-5]\n> ");
                        int rating = -1;
                        do {
                            try {
                                rating = Integer.parseInt(userInput.nextLine().trim());
                                if (rating <= 0 || rating >= 6) {
                                    System.out.print("Invalid input! Try again.\n> ");
                                }
                            } catch (NumberFormatException e) {
                                System.out.print("Invalid input! Try again.\n> ");
                            }
                        } while (rating <= 0 || rating >= 6);
                        requestBody.put(BackendUtils.BODY_FIELD_RATING, rating);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.NEW_RATING.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException | JSONException e) {
            System.err.println("\n! GuestConsole.main(): Error:\n" + e);
        } finally {
            if (guestConsole.getRequestSocket() != null) {
                try {
                    System.out.println("Closing down connection...");
                    guestConsole.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
