package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class GuestConsole {
    private enum MENU_OPTIONS {
        EXIT("Exit", "0"),
        UPLOAD_FILTERS_FILE("Upload new filters file", "1"),
        BOOK_RENTAL("Book a rental", "2"),
        RATE_RENTAL("Rate a rental", "3");

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

    private String connectUser() {
        // TODO: Add DAO lookup for user

        String username = "";
        System.out.print("\tWelcome back!\n" +
                "Enter username\n> ");
        do {
            username = userInput.nextLine().trim();
            if (!username.equals("guest")) {
                System.out.print("User not found. Try again\n> ");
            }
        } while (!username.equals("guest"));

        String input = "";
        System.out.print("Enter password\n> ");
        do {
            input = userInput.nextLine().trim();
            if (!input.equals("guest")) {
                System.out.print("Incorrect password. Try again\n> ");
            }
        } while (!input.equals("guest"));

        return username;
    }

    private void printRentalsList(ArrayList<Rental> rentals) {
        System.out.println("\n[Rentals List]\n");
        for (int i = 0; i < rentals.size(); i++) {
            System.out.printf("[%d] %s%n", i, rentals.get(i));
        }
        System.out.println("<-------- [End Of List] -------->");
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

        String username = guestConsole.connectUser();

        try {
            // Establish a connection
            Socket requestSocket = null;
            System.out.println("Connecting to server...");
            requestSocket = new Socket("localhost", BackendUtils.SERVER_PORT);
            guestConsole.setRequestSocket(requestSocket);
            DataOutputStream outputStream = guestConsole.getOutputStream();
            ObjectInputStream inputStream = guestConsole.getInputStream();

            JSONObject request, requestBody;
            ArrayList<Rental> rentalsFromLatestSearch = null;
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
                        rentalsFromLatestSearch = (ArrayList<Rental>) BackendUtils.serverToClient(inputStream);
                        if (rentalsFromLatestSearch == null) {
                            System.err.println("GuestConsole.main(): Could not receive host's rentals from Server.");
                            break;
                        }
                        guestConsole.printRentalsList(rentalsFromLatestSearch);
                        break;

                    case BOOK_RENTAL:
                        if (rentalsFromLatestSearch != null) {
                            System.out.print("Display rentals from latest search? (Y/N)\n> ");
                            String ans;
                            do {
                                ans = userInput.nextLine().trim();
                                if (!ans.equals("Y") && !ans.equals("N")) {
                                    System.out.print("Invalid input. Try again\n> ");
                                }
                            } while (!ans.equals("Y") && !ans.equals("N"));

                            if (ans.equals("Y")) {
                                guestConsole.printRentalsList(rentalsFromLatestSearch);
                                rentals = rentalsFromLatestSearch;
                            } else {
                                rentals = BackendUtils.getAllRentals(outputStream, inputStream, null);
                                if (rentals == null) {
                                    System.err.println("GuestConsole.main(): Error getting Rentals list.");
                                    break;
                                }
                            }
                        } else {
                            rentals = BackendUtils.getAllRentals(outputStream, inputStream, null);
                            if (rentals == null) {
                                System.err.println("GuestConsole.main(): Error getting Rentals list.");
                                break;
                            }
                        }

                        Rental rental = BackendUtils.chooseRentalFromList(rentals);
                        // Get start and end days of booking
                        requestBody = BackendUtils.getInputDatesAsJsonObject("mark available");
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rental.getId());

                        request = BackendUtils.createRequest(Requests.NEW_BOOKING.name(), requestBody.toString());
                        BackendUtils.clientToServer(outputStream, request.toString());
                        break;

                    case RATE_RENTAL:
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
