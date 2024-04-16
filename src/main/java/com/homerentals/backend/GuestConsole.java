package com.homerentals.backend;

import com.homerentals.domain.BookingReference;
import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    private DataOutputStream serverSocketDataOut = null;
    private ObjectInputStream serverSocketObjectIn = null;
    private static final Scanner userInput = new Scanner(System.in);

    public Socket getRequestSocket() {
        return this.requestSocket;
    }

    public void setRequestSocket(Socket requestSocket) throws IOException {
        this.requestSocket = requestSocket;
        try {
            this.serverSocketDataOut = new DataOutputStream(this.requestSocket.getOutputStream());
            this.serverSocketObjectIn = new ObjectInputStream(this.requestSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("GuestConsole.setRequestSocket(): Error setting outputs: " + e);
            throw e;
        }
    }

    public DataOutputStream getOutputStream() {
        return serverSocketDataOut;
    }

    public ObjectInputStream getInputStream() {
        return serverSocketObjectIn;
    }

    private String[] connectUser() {
        // TODO: Add DAO lookup for user

        String email = "";
        System.out.print("\tWelcome back!\n" +
                "Enter email\n> ");
        do {
            email = userInput.nextLine().trim();
            if (!email.equals("guest@example.com")) {
                System.out.print("User not found. Try again\n> ");
            }
        } while (!email.equals("guest@example.com"));

        String password = "";
        System.out.print("Enter password\n> ");
        do {
            password = userInput.nextLine().trim();
            if (!password.equals("guest")) {
                System.out.print("Incorrect password. Try again\n> ");
            }
        } while (!password.equals("guest"));

        return new String[]{email, password};
    }

    private void printRentalsList(ArrayList<Rental> rentals) {
        System.out.println("\n[Rentals List]\n");
        for (int i = 0; i < rentals.size(); i++) {
            System.out.printf("[%d] %s%n", i, rentals.get(i));
        }
        System.out.println("<-------- [End Of List] -------->");
    }

    private void bookNewRental(ArrayList<Rental> rentals, String email) throws IOException {
        // Ask user for new booking
        System.out.print("\n\nWould you like to book a rental? (Y/N)\n> ");
        String ans;
        do {
            ans = userInput.nextLine().trim();
            if (!ans.equalsIgnoreCase("Y") && !ans.equalsIgnoreCase("N")) {
                System.out.print("Invalid input. Try again\n> ");
            }
        } while (!ans.equalsIgnoreCase("Y") && !ans.equalsIgnoreCase("N"));

        if (ans.equalsIgnoreCase("N")) {
            return;
        }

        this.printRentalsList(rentals);
        Rental rental = BackendUtils.chooseRentalFromList(rentals);
        // Get start and end days of booking
        JSONObject requestBody = BackendUtils.getInputDatesAsJsonObject("book rental");
        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rental.getId());
        requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);

        JSONObject request = BackendUtils.createRequest(Requests.NEW_BOOKING.name(), requestBody.toString());
        BackendUtils.clientToServer(this.serverSocketDataOut, request.toString());

        BackendUtils.handleServerResponse(this.serverSocketObjectIn, "Booking successful!", "Booking failed. Try again another time.");
    }

    private void close() throws IOException {
        try {
            JSONObject request = BackendUtils.createRequest(Requests.CLOSE_CONNECTION.name(), "");
            BackendUtils.clientToServer(this.serverSocketDataOut, request.toString());
            this.serverSocketObjectIn.close();
            this.serverSocketDataOut.close();
            this.requestSocket.close();
        } catch (IOException e) {
            System.out.println("GuestConsole.close(): Error closing sockets: " + e);
            throw e;
        }
    }

    public static void main(String[] args) {
        GuestConsole guestConsole = new GuestConsole();

        String[] userInfo = guestConsole.connectUser();
        String email = userInfo[0];
        String password = userInfo[1];

        try {
            // Establish a connection
            Socket requestSocket = null;
            System.out.println("Connecting to server...");
            requestSocket = new Socket("localhost", BackendUtils.SERVER_PORT);
            guestConsole.setRequestSocket(requestSocket);
            DataOutputStream outputStream = guestConsole.getOutputStream();
            ObjectInputStream inputStream = guestConsole.getInputStream();

            JSONObject request, requestBody;
            ArrayList<Rental> rentals;
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
                        System.out.print("Enter file path for the .json file that contains the filters.\n> ");
                        String filePath = userInput.nextLine().trim();

                        // Read JSON file
                        JSONObject filters = BackendUtils.readFile(filePath);
                        if (filters == null) {
                            System.err.println("GuestConsole.main(): Error reading JSON File");
                            break;
                        }
                        requestBody = new JSONObject();
                        requestBody.put(BackendUtils.BODY_FIELD_FILTERS, filters);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_RENTALS.name(), requestBody.toString());
                        System.out.println(request);
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        rentals = (ArrayList<Rental>) BackendUtils.serverToClient(inputStream);
                        if (rentals == null) {
                            System.err.println("GuestConsole.main(): Could not receive rentals from Server.");
                            break;
                        }
                        guestConsole.printRentalsList(rentals);

                        try {
                            guestConsole.bookNewRental(rentals, email);
                        } catch (IOException e) {
                            System.err.println("GuestConsole.main(): Error booking rental: " + e);
                        }
                        break;

                    case VIEW_ALL_RENTALS:
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, null);
                        if (rentals == null) {
                            System.err.println("GuestConsole.main(): Error getting Rentals list.");
                            break;
                        }

                        try {
                            guestConsole.bookNewRental(rentals, email);
                        } catch (IOException e) {
                            System.err.println("GuestConsole.main(): Error booking rental: " + e);
                        }
                        break;

                    case RATE_STAY:
                        // Get all booking with no ratings
                        requestBody = new JSONObject();
                        requestBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email);
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS_WITH_NO_RATINGS.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        ArrayList<BookingReference> bookings = (ArrayList<BookingReference>) BackendUtils.serverToClient(inputStream);
                        if (bookings == null) {
                            System.err.println("GuestConsole.main(): Could not receive bookings from Server.");
                            break;
                        }
                        System.out.println("\n[Previous Stays]\n");
                        for (int i = 0; i < bookings.size(); i++) {
                            System.out.printf("[%d] %s%n", i, bookings.get(i));
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
                                if (bookingIndex < 0 || bookingIndex >= bookings.size()) {
                                    System.out.print("Invalid input. Try again\n> ");
                                }
                            } catch (NumberFormatException e) {
                                System.out.print("Invalid input. Try again\n> ");
                            }
                        } while (bookingIndex < 0 || bookingIndex >= bookings.size());
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, bookings.get(bookingIndex).getRentalId());
                        requestBody.put(BackendUtils.BODY_FIELD_BOOKING_ID, bookings.get(bookingIndex).getBookingId());

                        System.out.print("\nRate your stay [1-5]\n> ");
                        int rating = -1;
                        do {
                            try {
                                rating = Integer.parseInt(userInput.nextLine().trim());
                                if (rating <= 0 || rating >= 6) {
                                    System.out.print("Invalid input. Try again\n> ");
                                }
                            } catch (NumberFormatException e) {
                                System.out.print("Invalid input. Try again\n> ");
                            }
                        } while (rating <= 0 || rating >= 6);
                        requestBody.put(BackendUtils.BODY_FIELD_RATING, rating);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.NEW_RATING.name(), requestBody.toString());
                        System.out.println(request);
                        BackendUtils.clientToServer(outputStream, request.toString());
                        break;

                    default:
                        break;
                }
            }

        } catch (IOException | JSONException e) {
            System.out.println("GuestConsole.main(): Error: " + e);
            e.printStackTrace();

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
