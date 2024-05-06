package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.DomainUtils;
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
            System.err.println("\n! HostConsole.setRequestSocket(): Error setting outputs:\n" + e);
            throw e;
        }
    }

    public DataOutputStream getOutputStream() {
        return serverSocketDataOut;
    }

    public ObjectInputStream getInputStream() {
        return serverSocketObjectIn;
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
            BackendUtils.clientToServer(serverSocketDataOut, request.toString());
            this.serverSocketObjectIn.close();
            this.serverSocketDataOut.close();
            this.requestSocket.close();
        } catch (IOException e) {
            System.err.println("\n! HostConsole.close(): Error closing sockets:\n" + e);
            throw e;
        }
    }

    public static String printBookingInfo(Booking booking) {
        return String.format("Hosting user %s for [%s - %s]", booking.getGuestEmail(), DomainUtils.dateFormatter.format(booking.getStartDate()), DomainUtils.dateFormatter.format(booking.getEndDate()));
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
            ObjectInputStream inputStream = hostConsole.getInputStream();

            JSONObject request, requestBody;
            ArrayList<Rental> rentals;
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
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, username, true);
                        if (rentals == null) {
                            System.err.println("\n! HostConsole.main(): Error getting Rentals list.");
                            break;
                        }

                        Rental rental = BackendUtils.chooseRentalFromList(rentals);
                        // Get start and end days to mark available
                        requestBody = BackendUtils.getInputDatesAsJsonObject("mark available");
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rental.getId());

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        BackendUtils.handleServerResponse(hostConsole.getInputStream(), "Change successful!", "Change unsuccessful.\nCheck rental bookings and try again.");
                        break;

                    case VIEW_ALL_BOOKINGS:
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, username, false);
                        if (rentals == null) {
                            System.err.println("\n! HostConsole.main(): Error getting Rentals list.");
                            break;
                        }

                        for (Rental r : rentals) {
                            System.out.printf("%n[Rental: %s (%s)]%n%n", r.getRoomName(), r.getLocation());
                            for (Booking b : r.getBookings()) {
                                if (!b.hasPassed()) {
                                    System.out.printf("- %s%n", printBookingInfo(b));
                                }
                            }
                        }
                        System.out.println("<-------- [End Of List] -------->");
                        break;

                    case VIEW_AMOUNT_OF_BOOKINGS_PER_LOCATION:
                        // Get start and end days to show bookings
                        requestBody = BackendUtils.getInputDatesAsJsonObject("view bookings");

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());

                        // Receive response
                        ArrayList<BookingsByLocation> bookingsByLocation = (ArrayList<BookingsByLocation>) BackendUtils.serverToClient(inputStream);
                        if (bookingsByLocation == null) {
                            System.err.println("\n! HostConsole.main(): Could not receive host's rentals from Server.");
                            break;
                        }

                        // Display booking count per location
                        System.out.printf("%n[%s's Bookings Per Location]%n%n", username);
                        for (BookingsByLocation byLocation : bookingsByLocation) {
                            System.out.printf("- %s: %d%n%n", byLocation.getLocation(), byLocation.getBookingIds().size());
                        }
                        System.out.println("<-------- [End Of List] -------->");
                        break;

                    case VIEW_RENTALS:
                        rentals = BackendUtils.getAllRentals(outputStream, inputStream, username, true);
                        if (rentals == null) {
                            System.err.println("\n! HostConsole.main(): Error getting Rentals list.");
                            break;
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
