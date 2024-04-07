package com.homerentals.backend;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Scanner;

public class Client {
    // TODO: Replace System.out.println() with logger in log file.

    private Socket requestSocket = null;
    private DataOutputStream socketOutput = null;
    private DataInputStream socketInput = null;
    private static final Scanner userInput = new Scanner(System.in);

    public Socket getRequestSocket() {
        return this.requestSocket;
    }

    public void setRequestSocket(Socket requestSocket) throws IOException {
        this.requestSocket = requestSocket;
        try {
            this.socketOutput = new DataOutputStream(this.requestSocket.getOutputStream());
            this.socketInput = new DataInputStream(this.requestSocket.getInputStream());

        } catch (IOException e) {
            System.out.println("CLIENT: Error setting outputs: " + e);
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
            System.out.println("CLIENT: Error reading JSON File: " + e);
            return null;
        }
    }

    private JSONObject createRequest(String header, String body) {
        JSONObject request = new JSONObject();
        request.put("type", "request");
        request.put("header", header);
        request.put("body", body);

        return request;
    }

    private void sendSocketOutput(String msg) throws IOException {
        try {
            this.socketOutput.writeUTF(msg);
            this.socketOutput.flush();

        } catch (IOException e) {
            System.out.println("CLIENT: Error reading sending Socket Output: " + e);
            throw e;
        }
    }

    private String connectUser() {
        // TODO: Add DAO lookup for user

        String username = "";
        System.out.println("\tWelcome back!\n" +
                "Enter username\n> ");
        do {
            username = userInput.nextLine().trim();
            if (!username.equals("admin")) {
                System.out.println("User not found. Try again\n> ");
            }
        } while (!username.equals("admin"));

        String input = "";
        System.out.println("Enter password\n> ");
        do {
            input = userInput.nextLine().trim();
            if (!input.equals("admin")) {
                System.out.println("Incorrect password. Try again\n> ");
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
                "Dates should be in the format of: dd/MM/yyyy\n> %n", msg);
        boolean invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, dateFormatter);
                invalid = false;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put("startDate", input);

        System.out.printf("Enter end date to %s\n" +
                "Dates should be in the format of: dd/MM/yyyy\n> %n", msg);
        invalid = true;
        while (invalid) {
            try {
                input = userInput.nextLine().trim();
                LocalDate.parse(input, dateFormatter);
                invalid = false;

            } catch (DateTimeParseException e) {
                System.out.println("Invalid input. Try again\n> ");
                invalid = true;
            }
        }
        result.put("endDate", input);

        return result;
    }

    private void close() throws IOException {
        try {
            JSONObject request = this.createRequest(Requests.CLOSE_CONNECTION.name(), "");
            this.sendSocketOutput(request.toString());

            this.socketInput.close();
            this.socketOutput.close();
            this.requestSocket.close();

        } catch (IOException e) {
            System.out.println("CLIENT: Error closing sockets: " + e);
            throw e;
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        String username = client.connectUser();

        String filePath = args[0];

        try {
            // Establish a connection
            Socket requestSocket = null;
            System.out.println("Connecting to server...");
            requestSocket = new Socket("localhost", 8080);
            client.setRequestSocket(requestSocket);

            JSONObject request, requestBody;
            String input;
            boolean done = false;
            while (!done) {
                System.out.println("\n\n\t[MENU]");
                System.out.println("1. Add new rental");
                System.out.println("2. Update rental availability");
                System.out.println("3. View bookings");
                System.out.println("0. Exit");
                System.out.print("> ");

                String ans = userInput.nextLine().trim();
                switch (ans) {
                    case "0":
                        done = true;
                        break;

                    case "1":
                        // 1. Add new rental

                        // TODO: Have user provide path to
                        //  the json file of the new rental

                        // Read JSON file
                        JSONObject newRental = client.readFile(filePath);
                        if (newRental == null) {
                            System.out.println("CLIENT MAIN: Error reading JSON File");
                            break;
                        }

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = client.createRequest(Requests.NEW_RENTAL.name(), newRental.toString());
                        client.sendSocketOutput(request.toString());

                        break;

                    case "2":
                        // 2. Update rental availability

                        // TODO: Add rental search for user

                        // Get rental for update
                        input = "";
                        System.out.println("Enter rental name\n> ");
                        do {
                            input = userInput.nextLine().trim();
                            if (!input.equals("Cozy Rental")) {
                                System.out.println("Rental not found. Try again\n> ");
                            }
                        } while (!input.equals("Cozy Rental"));

                        // Get start and end days to mark available
                        requestBody = client.getInputDates("mark available");

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = client.createRequest(Requests.UPDATE_AVAILABILITY.name(), requestBody.toString());
                        client.sendSocketOutput(request.toString());

                        break;

                    case "3":
                        // 3. View bookings

                        // Get start and end days to show bookings
                        requestBody = client.getInputDates("view bookings");

                        // Get area to show bookings
                        input = "";
                        System.out.println("Enter area\n> ");
                        input = userInput.nextLine().trim();
                        requestBody.put("area", input);

                        // Write to socket
                        System.out.println("Writing to server...");
                        request = client.createRequest(Requests.GET_BOOKINGS.name(), requestBody.toString());
                        client.sendSocketOutput(request.toString());

                        break;

                    default:
                        break;
                }

                // Read response from server
                // inputStream = new ObjectInputStream(socket.getInputStream());
                // String msg = (String) inputStream.readObject();
            }

        } catch (IOException | JSONException e) {
            System.out.println("CLIENT MAIN: Error: " + e);
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
