package com.homerentals.backend;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private DataInputStream clientSocketIn = null;
    private ObjectOutputStream clientSocketOut = null;

    ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        try {
            this.clientSocketOut = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.clientSocketIn = new DataInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("ClientHandler(): Error setting up streams: " + e);
            throw e;
        }
    }

    private String readClientSocketInput() {
        try {
            return this.clientSocketIn.readUTF();
        } catch (IOException e) {
            System.out.println("ClientHandler.readClientSocketInput(): Error reading Client Socket input: " + e);
            return null;
        }
    }

    private void sendClientSocketOutput(Object obj) throws IOException {
        try {
            clientSocketOut.writeObject(obj);
            clientSocketOut.flush();
        } catch (IOException e) {
            System.err.println("ClientHandler.sendClientSocketOutput(): Error sending Client Socket output: " + e);
            throw e;
        }
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
                    System.err.println("ClientHandler.run(): Error reading Client Socket input");
                    break;
                }
                System.out.println(input);

                // Handle JSON input
                JSONObject inputJson = new JSONObject(input);
                String inputType = inputJson.getString(BackendUtils.MESSAGE_TYPE);
                JSONObject inputBody = new JSONObject(inputJson.getString(BackendUtils.MESSAGE_BODY));
                Requests inputHeader = Requests.valueOf(inputJson.getString(BackendUtils.MESSAGE_HEADER));

                JSONObject request;
                int workerPort, mapId, rentalId;
                switch (inputHeader) {
                    // Guest Requests
                    case GET_RENTALS:
                        // Add new mapId to requestBody
                        mapId = Server.getNextMapId();
                        inputBody.put(BackendUtils.BODY_FIELD_MAP_ID, mapId);
                        request = BackendUtils.createRequest(inputHeader.name(), inputBody.toString());
                        Server.sendMessageToWorkers(request.toString(), Server.ports); // broadcast request

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

                        // Send rentals to client
                        sendClientSocketOutput(mapResult.getRentals());
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
                        // Add new rentalId to requestBody
                        rentalId = Server.getNextRentalId();
                        inputBody.put(BackendUtils.BODY_FIELD_RENTAL_ID, rentalId);
                        request = BackendUtils.createRequest(inputHeader.name(), inputBody.toString());

                        // Forward new request to worker that will contain this rental
                        workerPort = Server.ports.get(Server.hash(rentalId));
                        Server.sendMessageToWorker(request.toString(), workerPort);
                        break;

                    case UPDATE_AVAILABILITY:
                        // Forward request, as it is,
                        // to worker that contains this rental
                        workerPort = Server.ports.get(Server.hash(inputBody.getInt(BackendUtils.BODY_FIELD_RENTAL_ID)));
                        Server.sendMessageToWorker(input, workerPort);
                        break;

                    case GET_BOOKINGS:
                        // TODO: Return result with MapReduce
                        break;

                    // Miscellaneous Requests
                    case CLOSE_CONNECTION:
                        System.out.println("ClientHandler.run(): Closing connection with client.");
                        running = false;
                        break;

                    default:
                        System.err.println("ClientHandler.run(): Request type not recognized.");
                        break;
                }
            }
        } catch (JSONException e) {
            System.err.println("ClientHandler.run(): Error: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("ClientHandler.run(): Could not retrieve result of MapReduce: " + e);
        } catch (IOException e) {
            System.err.println("ClientHandler.run(): Could not send MapReduce results to client: " + e);
        } finally {
            try {
                System.out.println("Closing thread");
                this.clientSocketIn.close();
                this.clientSocketOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}