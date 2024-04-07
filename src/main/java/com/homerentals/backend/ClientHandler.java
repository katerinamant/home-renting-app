package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ArrayList<Integer> ports = Server.ports;
    private DataInputStream clientSocketIn = null;

    ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
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

    private JSONObject createRequest(String header, String body) {
        JSONObject request = new JSONObject();
        request.put("type", "request");
        request.put("header", header);
        request.put("body", body);

        return request;
    }

    @Override
    public void run() {
        // Read data sent from client
        String input = null;
        try {
            while (true) {
                input = this.readClientSocketInput();
                if (input == null) {
                    System.err.println("ClientHandler.run(): Error reading Client Socket input");
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
                        int rentalId = Server.getNextRentalId();
                        JSONObject jsonBody = new JSONObject(inputBody);
                        jsonBody.put("rentalId", rentalId);
                        int workerPortIndex = Server.hash(rentalId);
                        System.out.println(rentalId);

                        JSONObject request = this.createRequest(inputHeader.name(), jsonBody.toString());

                        // Establish connection with worker
                        try (Socket workerSocket = new Socket("localhost", ports.get(workerPortIndex));
                             DataOutputStream workerSocketOutput = new DataOutputStream(workerSocket.getOutputStream()))
                        {
                            workerSocketOutput.writeUTF(request.toString());
                            workerSocketOutput.flush();
                        } catch (IOException e) {
                            System.err.println("ClientHandler.run(): Failed to connect to worker.");
                        }

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
            System.err.println("ClientHandler.run(): Error: " + e);
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