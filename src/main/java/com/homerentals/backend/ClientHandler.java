package com.homerentals.backend;

import com.homerentals.domain.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

class ClientHandler implements Runnable {
    private DataInputStream clientSocketIn = null;
    private DataOutputStream clientSocketOut = null;

    ClientHandler(Socket clientSocket) throws IOException {
        try {
            this.clientSocketOut = new DataOutputStream(clientSocket.getOutputStream());
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

    private void sendClientSocketOutput(String msg) throws IOException {
        try {
            System.out.printf("\n> Sending response : %s%n", msg);
            clientSocketOut.writeUTF(msg);
            clientSocketOut.flush();
        } catch (IOException e) {
            System.err.println("\n! ClientHandler.sendClientSocketOutput(): Error sending Client Socket output:\n" + e);
            throw e;
        }
    }

    private MapResult performMapReduce(Requests header, JSONObject body) throws InterruptedException {
        // Create MapReduce Request
        int mapId;
        synchronized (Server.mapIdSyncObj) {
            mapId = Server.getNextMapId();
        }
        body.put(BackendUtils.BODY_FIELD_MAP_ID, mapId);
        JSONObject request = BackendUtils.createRequest(header.toString(), body.toString());

        // Send request to all workers
        Server.broadcastMessageToWorkers(request.toString());

        // Check Server.mapReduceResults for Reducer response
        MapResult mapResult;
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
        String input;
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
                String emailString, passwordString, response, status;
                int workerId;
                JSONObject responseJson, responseBody, bookingInfo;
                switch (inputHeader) {
                    // Guest Requests
                    case SIGN_UP:
                        responseBody = new JSONObject();

                        Email email;
                        emailString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                        if (!Email.isValid(emailString) || Server.getUser(emailString) != null) {
                            // Invalid email
                            responseBody.put(BackendUtils.BODY_FIELD_ERROR, BackendUtils.BODY_FIELD_GUEST_EMAIL);
                            responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                            this.sendClientSocketOutput(responseJson.toString());
                            break;
                        } else {
                            email = new Email(emailString);
                        }

                        Password password;
                        passwordString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_PASSWORD);
                        if (!Password.isValid(passwordString)) {
                            // Invalid password
                            responseBody.put(BackendUtils.BODY_FIELD_ERROR, BackendUtils.BODY_FIELD_GUEST_PASSWORD);
                            responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                            this.sendClientSocketOutput(responseJson.toString());
                            break;
                        } else {
                            password = new Password(passwordString);
                        }

                        PhoneNumber phoneNumber;
                        String phoneNumberString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_PHONE_NUMBER);
                        if (!PhoneNumber.isValid(phoneNumberString)) {
                            // Invalid phone number
                            responseBody.put(BackendUtils.BODY_FIELD_ERROR, BackendUtils.BODY_FIELD_GUEST_PHONE_NUMBER);
                            responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                            this.sendClientSocketOutput(responseJson.toString());
                            break;
                        } else {
                            phoneNumber = new PhoneNumber(phoneNumberString);
                        }

                        // Valid inputs
                        String firstName = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_FIRST_NAME);
                        String lastName = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_LAST_NAME);
                        Server.addUser(email, password, firstName, lastName, phoneNumber);

                        // Send "OK" response
                        responseBody.put(BackendUtils.BODY_FIELD_STATUS, "OK");
                        responseBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, email.toString());
                        responseBody.put(BackendUtils.BODY_FIELD_GUEST_PHONE_NUMBER, phoneNumber.getPhoneNumber());
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case CHECK_CREDENTIALS:
                        emailString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                        passwordString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_PASSWORD);

                        // Send response
                        responseBody = new JSONObject();
                        boolean userExists = Server.userExists(emailString, passwordString);
                        if (userExists) {
                            responseBody.put(BackendUtils.BODY_FIELD_STATUS, "OK");
                            GuestAccount guestAccount = Server.getUser(emailString);
                            responseBody.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, guestAccount.getEmail().toString());
                            responseBody.put(BackendUtils.BODY_FIELD_GUEST_PHONE_NUMBER, guestAccount.getPhoneNumber().getPhoneNumber());
                        } else {
                            responseBody.put(BackendUtils.BODY_FIELD_STATUS, "ERROR");
                        }
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case GET_RENTALS:
                        // MapReduce
                        mapResult = this.performMapReduce(inputHeader, inputBody);

                        // Create JSON response
                        responseBody = new JSONObject();
                        JSONArray rentals = new JSONArray();
                        for (Rental rental : mapResult.getRentals()) {
                            rentals.put(rental.toJSON());
                        }
                        responseBody.put(BackendUtils.BODY_FIELD_RENTALS, rentals);
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        // Send rentals to client
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case CHECK_AVAILABILITY:
                        // Forward request to worker that contains this rental
                        int rentalId = inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID);
                        workerId = Server.hash(rentalId);
                        response = Server.sendMessageToWorkerAndWaitForResponse(input, workerId);
                        if (response == null) {
                            break;
                        }

                        // Forward response to client
                        this.sendClientSocketOutput(response);
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
                        responseJson = BackendUtils.createResponse(inputHeader.name(), simplifiedResponseBody.toString());
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case GET_BOOKINGS_WITH_NO_RATINGS:
                        // Get info from Server.GuestAccountDAO
                        emailString = inputBody.getString(BackendUtils.BODY_FIELD_GUEST_EMAIL);
                        ArrayList<BookingReference> bookingsArray = Server.getGuestBookings(emailString);
                        if (bookingsArray == null) {
                            System.err.println("\n! ClientHandle.run(): User " + emailString + " not found.");
                            break;
                        }
                        System.out.println("\n> Sending to client: " + bookingsArray);

                        // Create JSON response
                        responseBody = new JSONObject();
                        JSONArray bookings = new JSONArray();
                        for (BookingReference bookingReference : bookingsArray) {
                            bookings.put(bookingReference.toJSON());
                        }
                        responseBody.put(BackendUtils.BODY_FIELD_BOOKINGS, bookings);
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        // Send booking references to client
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case NEW_RATING:
                        // Forward request, as it is,
                        // to worker that contains this rental
                        workerId = Server.hash(inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID));
                        response = Server.sendMessageToWorkerAndWaitForResponse(input, workerId);
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

                    case GET_ALL_BOOKINGS:
                        // Get all rentals
                        mapResult = this.performMapReduce(inputHeader, inputBody);

                        // Create JSON response
                        // responseBody =
                        // { bookings:
                        //      [JSONArray of
                        //          {   rentalString,
                        //              [JSONArray of
                        //                  {bookingString}
                        //              ]
                        //          }
                        //       ]
                        // }
                        responseBody = new JSONObject();
                        JSONArray rentalsWithBookings = new JSONArray();
                        JSONObject rentalInfoAndBookings;
                        JSONArray bookingInfoOfThisRental;
                        for (Rental rental : mapResult.getRentals()) {
                            rentalInfoAndBookings = new JSONObject();
                            rentalInfoAndBookings.put(BackendUtils.BODY_FIELD_RENTAL_STRING, rental.toString());
                            bookingInfoOfThisRental = new JSONArray();
                            for (Booking booking : rental.getBookings()) {
                                if (!booking.hasPassed()) {
                                    bookingInfo = new JSONObject();
                                    bookingInfo.put(BackendUtils.BODY_FIELD_BOOKING_STRING, booking.toString());
                                    bookingInfoOfThisRental.put(bookingInfo);
                                }
                            }
                            rentalInfoAndBookings.put(BackendUtils.BODY_FIELD_BOOKINGS, bookingInfoOfThisRental);
                            rentalsWithBookings.put(rentalInfoAndBookings);
                        }
                        responseBody.put(BackendUtils.BODY_FIELD_RENTALS_WITH_BOOKINGS, rentalsWithBookings);
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        // Send booking references to client
                        this.sendClientSocketOutput(responseJson.toString());
                        break;

                    case GET_BOOKINGS_BY_LOCATION:
                        // MapReduce
                        mapResult = this.performMapReduce(inputHeader, inputBody);

                        // Create JSON response
                        responseBody = new JSONObject();
                        JSONArray bookingsByLocation = new JSONArray();
                        JSONObject location;
                        for (BookingsByLocation byLocation : mapResult.getBookingsByLocation()) {
                            location = new JSONObject();
                            location.put(BackendUtils.BODY_FIELD_BY_LOCATION, byLocation.toString());
                            bookingsByLocation.put(location);
                        }
                        responseBody.put(BackendUtils.BODY_FIELD_BOOKINGS_BY_LOCATION, bookingsByLocation);
                        responseJson = BackendUtils.createResponse(inputHeader.name(), responseBody.toString());
                        // Send amount of bookings per location to client
                        this.sendClientSocketOutput(responseJson.toString());
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
