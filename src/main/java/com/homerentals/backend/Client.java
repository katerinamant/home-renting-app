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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Client {
    // TODO: Replace System.out.println() with logger in log file.

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
            System.err.println("Client.sendSocketOutput(): Error reading sending Socket Output: " + e.getMessage());
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
    private JSONObject getInputDates(String msg) {
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
        JSONObject result = new JSONObject();
        String input = "";

        System.out.printf("Enter start date to %s\n" +
                "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
        boolean invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, dateFormatter);
                invalid = false;
            } catch (DateTimeParseException e) {
                System.out.print("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put("startDate", input);

        System.out.printf("Enter end date to %s\n" +
                "Dates should be in the format of: dd/MM/yyyy\n> ", msg);
        invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, dateFormatter);
                invalid = false;

            } catch (DateTimeParseException e) {
                System.out.print("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put("endDate", input);

        return result;
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
        Client client = new Client();

        String username = client.connectUser();

        try {
            // Establish a connection
            Socket requestSocket = null;
            System.out.println("Connecting to server...");
            requestSocket = new Socket("localhost", BackendUtils.SERVER_PORT);
            client.setRequestSocket(requestSocket);

            JSONObject request, requestBody;
            String input;
            boolean done = false;
            while (!done) {
                System.out.println("\n\n\t[MENU]");
                System.out.println("1. Upload new rental file");
                System.out.println("2. Update rental availability");
                System.out.println("3. View bookings");
                System.out.println("4. View my rentals");
                System.out.println("0. Exit");
                System.out.print("> ");

                String ans = userInput.nextLine().trim();
                switch (ans) {
                    case "0":
                        done = true;
                        break;

                    case "1":
                        // 1. Upload new rental file

                        System.out.print("Enter file path for the .json file that contains the rental information.\n> ");
                        String filePath = userInput.nextLine().trim();

                        // Read JSON file
                        JSONObject newRental = client.readFile(filePath);
                        if (newRental == null) {
                            System.out.println("Client.main(): Error reading JSON File");
                            break;
                        }

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.NEW_RENTAL.name(), newRental.toString());
                        System.out.println(request.toString());
                        client.sendSocketOutput(request.toString());

                        break;

                    case "2":
                        // 2. Update rental availability

                        // TODO: Add rental search for user

                        // Get rental for update
                        input = "";
                        System.out.print("Enter rental name\n> ");
                        do {
                            input = userInput.nextLine().trim();
                            if (!input.equals("Cozy Rental")) {
                                System.out.print("Rental not found. Try again\n> ");
                            }
                        } while (!input.equals("Cozy Rental"));

                        // Get start and end days to mark available
                        requestBody = client.getInputDates("mark available");

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), requestBody.toString());
                        client.sendSocketOutput(request.toString());

                        break;

                    case "3":
                        // 3. View bookings

                        // Get start and end days to show bookings
                        requestBody = client.getInputDates("view bookings");

                        // Get location to show bookings
                        input = "";
                        System.out.print("Enter location\n> ");
                        input = userInput.nextLine().trim();
                        requestBody.put("location", input);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = BackendUtils.createRequest(Requests.GET_BOOKINGS.name(), requestBody.toString());
                        client.sendSocketOutput(request.toString());
                        break;

                    case "4":
                        // 4. View my rentals

                        // Create and send request
                        JSONObject filters = new JSONObject();
                        JSONObject body = new JSONObject();
                        body.put(BackendUtils.BODY_FIELD_FILTERS, filters);
                        request = BackendUtils.createRequest(Requests.GET_RENTALS.name(), body.toString());
                        client.sendSocketOutput(request.toString());

                        // Receive response
                        ArrayList<Rental> rentals = (ArrayList<Rental>) client.readSocketObjectInput();
                        if (rentals == null) {
                            System.err.println("Client.main(): Could not receive host's rentals from Server.");
                            break;
                        }

                        System.out.printf("%n[%s's Rentals List]%n%n", username);
                        for(int i=0; i<rentals.size(); i++) {
                            System.out.printf("[%d] %s%n", i, rentals.get(i));
                        }
                        System.out.println("<-------- [End Of List] -------->");
                        break;

                    default:
                        break;
                }

                // Read response from server
                // inputStream = new ObjectInputStream(socket.getInputStream());
                // String msg = (String) inputStream.readObject();
            }

        } catch (IOException | JSONException e) {
            System.out.println("Client.main(): Error: " + e);
            e.printStackTrace();

        } finally {
            if (client.getRequestSocket() != null) {
                try {
                    System.out.println("Closing down connection...");
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
