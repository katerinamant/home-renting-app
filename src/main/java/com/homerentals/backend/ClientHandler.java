package com.homerentals.backend;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ArrayList<Integer> ports;
    private DataInputStream clientSocketIn = null;

    ClientHandler(Socket clientSocket, ArrayList<Integer> ports) throws IOException {
        this.clientSocket = clientSocket;
        this.ports = ports;
        try {
            this.clientSocketIn = new DataInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("CLIENT HANDLER: Error setting up streams: " + e);
            throw e;
        }
    }

    private String readClientSocketInput() {
        try {
            return this.clientSocketIn.readUTF();
        } catch (IOException e) {
            System.out.println("CLIENT HANDLER: Error reading Client Socket input: " + e);
            return null;
        }
    }

    @Override
    public void run() {
        // Read data sent from client
        String input = null;
        try {
            while (true) {
                input = this.readClientSocketInput();
                if (input == null) {
                    System.out.println("REQUEST HANDLER RUN: Error reading Client Socket input");
                    break;
                }
                System.out.println(input);

                // Handle JSON input
                JSONObject inputJson = new JSONObject(input);
                String inputType = inputJson.getString("type");
                String inputBody = inputJson.getString("body");
                Requests inputHeader = Requests.valueOf(inputJson.getString("header"));

                switch (inputHeader) {
                    // Guest Requests
                    case GET_RENTALS:
                        // TODO: Return result with MapReduce
                        break;

                    case NEW_BOOKING:
                         /*TODO: Choose worker to forward request
                            based on hash function on rental*/
                        break;

                    case NEW_RATING:
                        /* TODO: Choose worker to forward request
                            based on hash function on rental*/
                        break;

                    // Host Requests
                    case NEW_RENTAL:
                        /* TODO: Choose worker to forward request
                            based on hash function on rental*/
                        break;

                    case UPDATE_AVAILABILITY:
                        /* TODO: Choose worker to forward request
                            based on hash function on rental*/
                        break;

                    case GET_BOOKINGS:
                        // TODO: Return result with MapReduce
                        break;

                    // Miscellaneous Requests
                    case CLOSE_CONNECTION:
                        System.out.println("ClientHandler.run(): Closing connection with client.");
                        break;

                    default:
                        System.err.println("ClientHandler.run(): Request type not recognized.");
                        break;
                }
            }
        } catch (JSONException e) {
            System.out.println("REQUEST HANDLER RUN: Error: " + e);
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing thread");
                this.clientSocketIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}