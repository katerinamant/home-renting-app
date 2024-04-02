package com.homerentals;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Worker {
    private final ServerSocket workerSocket;
    private final Socket masterSocket;
    private DataOutputStream out = null;
    private DataInputStream in = null;
    // private int id;
    private final ArrayList<Rental> rentals = new ArrayList<Rental>();

    public Worker(ServerSocket workerSocket, Socket masterSocket) {
        this.workerSocket = workerSocket;
        this.masterSocket = masterSocket;
        try {
            this.out = new DataOutputStream(this.masterSocket.getOutputStream());
            this.in = new DataInputStream(this.masterSocket.getInputStream());

        } catch (IOException e) {
            String msg = "Error setting up streams";
            throw new RuntimeException(msg, e.getCause());
        }
    }

    public ArrayList<Rental> getRentals() {
        return rentals;
    }

    private String readMasterSocketInput() {
        try {
            return in.readUTF();

        } catch (IOException e) {
            String msg = "Error reading input";
            throw new RuntimeException(msg, e.getCause());
        }
    }

    public static void main(String[] args) {
        Worker worker = null;
        ServerSocket workerSocket = null;

        try {
            // Worker is listening on port 1000
            workerSocket = new ServerSocket(1000, 10);
            workerSocket.setReuseAddress(true);

            // Accept Master connection
            Socket masterSocket = workerSocket.accept();

            worker = new Worker(workerSocket, masterSocket);

            // Displaying that master server connected
            // to worker
            System.out.println("> Master connected: " + masterSocket.getInetAddress().getHostAddress());

            String input;
            while (true) {
                input = worker.readMasterSocketInput();
                System.out.println(input);

                // Handle JSON input
                JSONObject inputJson = new JSONObject(input);
                String inputType = inputJson.getString("type");
                String inputHeader = inputJson.getString("header");
                String inputBody = inputJson.getString("body");

                if (inputType.equals("request") && inputHeader.equals("close-connection")) {
                    System.out.println("Stop accepting from master");
                    break;
                }

                // Create a new thread object
                // to handle this request separately
                RequestHandler requestThread = new RequestHandler(worker.getRentals(), inputJson);
                new Thread(requestThread).start();
            }

        } catch (IOException e) {
            System.err.println("Worker IO Error !\n");
            e.printStackTrace();

        } finally {
            if (workerSocket != null) {
                try {
                    workerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class RequestHandler implements Runnable {
        private final ArrayList<Rental> rentals;
        private final JSONObject requestJson;

        public RequestHandler(ArrayList<Rental> rentals, JSONObject requestJson) {
            this.rentals = rentals;
            this.requestJson = requestJson;
        }

        private Rental jsonToRentalObject(String input) {
            try {
                // Create Rental object from JSON
                JSONObject jsonObject = new JSONObject(input);
                String roomName = jsonObject.getString("roomName");
                String area = jsonObject.getString("area");
                double pricePerNight = jsonObject.getDouble("pricePerNight");
                int numOfPersons = jsonObject.getInt("numOfPersons");
                int numOfReviews = jsonObject.getInt("numOfReviews");
                int sumOfReviews = jsonObject.getInt("sumOfReviews");
                String startDate = jsonObject.getString("startDate");
                String endDate = jsonObject.getString("endDate");
                String imagePath = jsonObject.getString("imagePath");
                Rental rental = new Rental(null, roomName, area, pricePerNight, numOfPersons, numOfReviews, sumOfReviews, startDate, endDate, imagePath);
//              System.out.println(rental.getRoomName());
//			    System.out.println(rental.getStars());

                return rental;

            } catch (JSONException e) {
                // String is not valid JSON object
                String msg = "Error creating JSON object\n";
                throw new RuntimeException(msg, e.getCause());
            }
        }

        @Override
        public void run() {
            // Handle JSON input
            String inputType = this.requestJson.getString("type");
            String inputHeader = this.requestJson.getString("header");
            String inputBody = this.requestJson.getString("body");

            if (inputType.equals("request") && inputHeader.equals("new-rental")) {
                try {
                    // Create Rental object from JSON
                    Rental rental = this.jsonToRentalObject(inputBody);

                    synchronized (this.rentals) {
                        System.out.println("lock");
                        System.out.println(this.rentals);
                        this.rentals.add(rental);
                    }
                    System.out.println("done");
                    System.out.println(this.rentals);

                } catch (JSONException e) {
                    // String is not valid JSON object
                    String msg = "Error creating JSON object";
                    throw new RuntimeException(msg, e.getCause());
                }
            }
        }
    }
}
