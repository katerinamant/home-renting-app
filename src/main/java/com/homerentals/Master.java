package com.homerentals;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Master {
    public static void main(String[] args) {
        ServerSocket server = null;

        try {
            // Server is listening on port 8080
            server = new ServerSocket(8080);
            server.setReuseAddress(true);

            // Handle client requests
            while (true) {
                Socket client = server.accept();

                // Displaying that new client is connected
                // to server
                // System.out.println("New client connected " + client.getInetAddress().getHostAddress());

                // Create a new thread object
                // to handle this client separately
                ClientHandler clientThread = new ClientHandler(client);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        private String readSocketInput() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                StringBuilder sb = new StringBuilder();

                for (int chr = reader.read(); reader.ready(); chr = reader.read()) {
                    sb.append((char) chr);
                }
                return sb.toString();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void jsonToObject(String input) {
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
                Rental rental = new Rental(roomName, area, pricePerNight, numOfPersons, numOfReviews, sumOfReviews, startDate, endDate, imagePath);
//              System.out.println(rental.getRoomName());
//			    System.out.println(rental.getStars());

            } catch (JSONException e) {
                // String is not valid JSON object
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            // Read data sent from client
            String input = null;
            try {
                input = this.readSocketInput();
                System.out.println(input);

                // Create Rental object from JSON
                this.jsonToObject(input);
                
            } catch (RuntimeException e) {
                String msg = String.format("Was not able to process input from socket %s", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg);
            }
        }
    }
}
