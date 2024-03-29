package com.homerentals;

import com.homerentals.domain.Rental;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Master {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            // Server is listening on port 8080
            serverSocket = new ServerSocket(8080, 10);
            serverSocket.setReuseAddress(true);

            // Handle client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Displaying that new client is connected
                // to server
                System.out.println("> New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread object
                // to handle this client separately
                ClientHandler clientThread = new ClientHandler(clientSocket);
                new Thread(clientThread).start();
            }

        } catch (IOException e) {
            System.err.println("Server IO Error !\n");
            e.printStackTrace();

        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private DataOutputStream out = null;
        private DataInputStream in = null;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.out = new DataOutputStream(this.clientSocket.getOutputStream());
                this.in = new DataInputStream(this.clientSocket.getInputStream());

            } catch (IOException e) {
                String msg = String.format("\tClient %s | Error creating streams:%n", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg, e.getCause());
            }
        }

        private String readSocketInput() {
            try {
                return in.readUTF();

            } catch (IOException e) {
                String msg = String.format("\tClient %s | Error reading input%n", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg, e.getCause());
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
                String msg = String.format("\tClient %s | Error creating JSON object%n", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg, e.getCause());
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
                String title = String.format("ERROR Client %s%n", this.clientSocket.getInetAddress().getHostAddress());
                System.err.println(title);
                e.printStackTrace();

            } finally {
                try {
                    in.close();
                    out.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
