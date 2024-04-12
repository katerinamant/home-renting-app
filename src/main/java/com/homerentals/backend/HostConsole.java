package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class HostConsole {
    // TODO: Replace System.out.println() with logger in log file.

    private enum MENU_OPTIONS {
        EXIT("Exit", "0"),
        UPLOAD_RENTAL_FILE("Upload new rental file", "1"),
        UPDATE_RENTAL_AVAILABILITY("Update rental availability", "2"),
        VIEW_RENTAL_BOOKINGS("View rental bookings", "3"),
        VIEW_RENTALS("View my rentals", "4");

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
            return String.format("%d. %s", num, getOption(String.valueOf(num)));
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
            System.out.println("Client.setRequestSocket(): Error setting outputs: " + e);
            throw e;
        }
    }

    private JSONObject readFile(String path) {
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

    private void sendSocketOutput(String msg) throws IOException {
        try {
            this.serverSocketDataOut.writeUTF(msg);
            this.serverSocketDataOut.flush();
        } catch (IOException e) {
            System.err.println("Client.sendSocketOutput(): Error sending Socket Output: " + e.getMessage());
            throw e;
        }
    }

    private Object readSocketObjectInput() {
        try {
            return this.serverSocketObjectIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client.readSocketObjectInput(): Could not read object from server input stream: " + e.getMessage());
            return null;
        }
    }

    private String connectUser() {
        // TODO: Add DAO lookup for user

        String username = "";
        System.out.print("\tWelcome back!\n" +
                "Enter username\n> ");
        do {
            username = userInput.nextLine().trim();
            if (!username.equals("admin")) {
                System.out.print("User not found. Try again\n> ");
            }
        } while (!username.equals("admin"));

        String input = "";
        System.out.print("Enter password\n> ");
        do {
            input = userInput.nextLine().trim();
            if (!input.equals("admin")) {
                System.out.print("Incorrect password. Try again\n> ");
            }
        } while (!input.equals("admin"));

        return username;
    }

    /**
     * @return JSONObject : {"startDate", "endDate"}
     */
    private JSONObject getInputDatesAsJsonObject(String msg) {
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

    private ArrayList<Rental> getAllRentals(String username) throws IOException {
        // Create and send request
        JSONObject filters = new JSONObject();
        JSONObject body = new JSONObject();
        body.put(BackendUtils.BODY_FIELD_FILTERS, filters);
        JSONObject request = BackendUtils.createRequest(Requests.GET_RENTALS.name(), body.toString());
        try {
            this.sendSocketOutput(request.toString());
        } catch (IOException e) {
            System.err.println("Client.getAllRentals(): Error sending Socket Output: " + e.getMessage());
            throw e;
        }

        // Receive response
        ArrayList<Rental> rentals = (ArrayList<Rental>) this.readSocketObjectInput();
        if (rentals == null) {
            System.err.println("Client.main(): Could not receive host's rentals from Server.");
            return null;
        }

        System.out.printf("%n[%s's Rentals List]%n%n", username);
        for(int i=0; i<rentals.size(); i++) {
            System.out.printf("[%d] %s%n", i, rentals.get(i));
        }
        System.out.println("<-------- [End Of List] -------->");
        return rentals;
    }

    private void close() throws IOException {
        try {
            JSONObject request = BackendUtils.createRequest(Requests.CLOSE_CONNECTION.name(), "");
            this.sendSocketOutput(request.toString());
            this.serverSocketObjectIn.close();
            this.serverSocketDataOut.close();
            this.requestSocket.close();

        } catch (IOException e) {
            System.out.println("Client.close(): Error closing sockets: " + e);
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
            requestSocket = new Socket("localhost", BackendUtils.SERVER_PORT);
            hostConsole.setRequestSocket(requestSocket);

            JSONObject request, requestBody;
            ArrayList<Rental> rentals;
            boolean done = false;
            while (!done) {
                System.out.println("\n\n\t[MENU]");
                for (int i=1; i < MENU_OPTIONS.values().length; i++) {
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
                        System.out.print("Enter file path for the .json file that contains the rental information.\n> ");
                        String filePath = userInput.nextLine().trim();

                        // Read JSON file
                        JSONObject newRental = hostConsole.readFile(filePath);
                        if (newRental == null) {
                            System.err.println("Client.main(): Error reading JSON File");
                            break;
                        }

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.NEW_RENTAL.name(), newRental.toString());
                        System.out.println(request);
                        hostConsole.sendSocketOutput(request.toString());

                        break;

                    case UPDATE_RENTAL_AVAILABILITY:
                        // Print rentals list
                        rentals = hostConsole.getAllRentals(username);
                        if (rentals == null) {
                            System.err.println("Client.main(): Error getting Rentals list.");
                            break;
                        }

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

                        // Get start and end days to mark available
                        requestBody = hostConsole.getInputDatesAsJsonObject("mark available");
                        requestBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentals.get(rentalIndex).getId());

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), requestBody.toString());
                        hostConsole.sendSocketOutput(request.toString());
                        break;

                    case VIEW_RENTAL_BOOKINGS:
                        // Get start and end days to show bookings
                        requestBody = hostConsole.getInputDatesAsJsonObject("view bookings");

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS.name(), requestBody.toString());
                        hostConsole.sendSocketOutput(request.toString());
                        break;

                    case VIEW_RENTALS:
                        rentals = hostConsole.getAllRentals(username);
                        if (rentals == null) {
                            System.err.println("Client.main(): Error getting Rentals list.");
                        }
                        break;

                    default:
                        break;
                }
            }

        } catch (IOException | JSONException e) {
            System.out.println("Client.main(): Error: " + e);
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
