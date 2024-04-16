package com.homerentals.backend;

import com.homerentals.domain.BookingReference;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

class ClientHandler implements Runnable {
    private DataInputStream clientSocketIn = null;
    private ObjectOutputStream clientSocketOut = null;

    ClientHandler(Socket clientSocket) throws IOException {
        try {
            this.clientSocketOut = new ObjectOutputStream(clientSocket.getOutputStream());
            this.clientSocketIn = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("\n! ClientHandler(): Error setting up streams:\n" + e);
            throw e;
        }
    }

    private String readClientSocketInput() {
        try {
            return this.clientSocketIn.readUTF();
        } catch (IOException e) {
            System.err.println("\n! ClientHandler.readClientSocketInput(): Error reading Client Socket input:\n" + e);
            return null;
        }
    }

    private void sendClientSocketOutput(Object obj) throws IOException {
        try {
            clientSocketOut.writeObject(obj);
            clientSocketOut.flush();
        } catch (IOException e) {
            System.err.println("\n! ClientHandler.sendClientSocketOutput(): Error sending Client Socket output:\n" + e);
            throw e;
        }
    }

    private MapResult performMapReduce(Requests header, JSONObject body) throws InterruptedException {
        int requestId = body.getInt(BackendUtils.BODY_FIELD_REQUEST_ID);
        // Create MapReduce Request
        int mapId = Server.getNextMapId();
        body.put(BackendUtils.BODY_FIELD_MAP_ID, mapId);
        JSONObject request = BackendUtils.createRequest(header.toString(), body.toString(), requestId);

        // Send request to all workers
        Server.sendMessageToWorkers(request.toString(), Server.ports);

        // Check Server.mapReduceResults for Reducer response
        MapResult mapResult = null;
        synchronized (ReducerHandler.syncObj) {
            while (!Server.mapReduceResults.containsKey(mapId)) {
                ReducerHandler.syncObj.wait();
            }
            mapResult = Server.mapReduceResults.get(mapId);
            Server.mapReduceResults.remove(mapId);
        }

        if (mapResult == null) {
            throw new InterruptedException();
        }

        return mapResult;
    }

    @Override
    public void run() {
        // Read data sent from client
        String input = null;
        boolean running = true;
        try {
            while (running) {
                input = this.readClientSocketInput();
                if (input == null) {
                    System.err.println("\n! ClientHandler.run(): Error reading Client Socket input");
                    break;
                }
                System.out.println("\n> Received: " + input);

                // Handle JSON input
                JSONObject inputJson = new JSONObject(input);
                String inputType = inputJson.getString(BackendUtils.MESSAGE_TYPE);
                JSONObject inputBody = new JSONObject(inputJson.getString(BackendUtils.MESSAGE_BODY));
                Requests inputHeader = Requests.valueOf(inputJson.getString(BackendUtils.MESSAGE_HEADER));

                MapResult mapResult;
                String response, status;
                JSONObject responseJson, responseBody;
                switch (inputHeader) {
                    // Guest Requests
                    case GET_RENTALS:
                        // MapReduce
                        mapResult = this.performMapReduce(inputHeader, inputBody);

                        // Send rentals to client
                        this.sendClientSocketOutput(mapResult.getRentals());
                        break;

                    case NEW_BOOKING:
                        responseBody = BackendUtils.executeNewBookingRequest(inputBody, inputHeader.name());
                        if (responseBody == null) {
                            // Communication with the worker was unsuccessful
                            break;
                        }

                        // Communication with the worker was successful.
                        // If the booking was successful,
                        // it was added to the guest's list
                        // in the executeNewBookingRequest() function.

                        // Send simplified response to client
                        JSONObject simplifiedResponseBody = new JSONObject();
                        simplifiedResponseBody.put(BackendUtils.BODY_FIELD_STATUS, responseBody.getString(BackendUtils.BODY_FIELD_STATUS));
                        responseJson = BackendUtils.createResponse(inputHeader.name(), simplifiedResponseBody.toString(), responseBody.getInt(BackendUtils.BODY_FIELD_REQUEST_ID));
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case GET_BOOKINGS_WITH_NO_RATINGS:
                        // Get info from Server.GuestAccountDAO
                        String email = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                        ArrayList<BookingReference> bookings = Server.getGuestBookings(email);
                        System.out.println("\n> Sending to client: " + bookings);

                        // Send booking references to client
                        this.sendClientSocketOutput(bookings);
                        break;

                    case NEW_RATING:
                        // Forward request, as it is,
                        // to worker that contains this rental
                        int workerPort = Server.ports.get(Server.hash(inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID)));
                        response = Server.sendMessageToWorkerAndWaitForResponse(input, workerPort);
                        if (response == null) {
                            break;
                        }

                        // Handle JSON response
                        responseJson = new JSONObject(response);
                        responseBody = new JSONObject(responseJson.getString(BackendUtils.MESSAGE_BODY));
                        status = responseBody.getString(BackendUtils.BODY_FIELD_STATUS);
                        if (status.equals("OK")) {
                            System.out.println("\n> Rating was successful.");
                            // If the rating was successful
                            // remove booking from guest's list
                            String bookingId = responseBody.getString(BackendUtils.BODY_FIELD_BOOKING_ID);
                            String guestEmail = responseBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                            Server.rateGuestsBooking(guestEmail, bookingId);
                        }
                        break;

                    // Host Requests
                    case NEW_RENTAL:
                        BackendUtils.executeNewRentalRequest(inputBody, inputHeader.name());
                        break;

                    case UPDATE_AVAILABILITY:
                        response = BackendUtils.executeUpdateAvailability(input, inputBody);
                        this.sendClientSocketOutput(response);
                        break;

                    case GET_BOOKINGS:
                        // MapReduce
                        mapResult = this.performMapReduce(inputHeader, inputBody);

                        // Send amount of bookings per location to client
                        this.sendClientSocketOutput(mapResult.getBookingsByLocation());
                        break;

                    // Miscellaneous Requests
                    case CLOSE_CONNECTION:
                        System.out.println("\n> ClientHandler.run(): Closing connection with client.");
                        running = false;
                        break;

                    default:
                        System.err.println("\n! ClientHandler.run(): Request type not recognized.");
                        break;
                }
            }
        } catch (JSONException e) {
            System.err.println("\n! ClientHandler.run(): JSON Exception:\n" + e);
        } catch (InterruptedException e) {
            System.err.println("\n! ClientHandler.run(): Could not retrieve result of MapReduce:\n" + e);
        } catch (IOException e) {
            System.err.println("\n! ClientHandler.run(): Could not send MapReduce results to client:\n" + e);
        } finally {
            try {
                System.out.println("\n> Closing thread...");
                this.clientSocketIn.close();
                this.clientSocketOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
