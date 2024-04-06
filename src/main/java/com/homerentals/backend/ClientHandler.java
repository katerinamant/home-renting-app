package com.homerentals.backend;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
                String inputHeader = inputJson.getString("header");
                String inputBody = inputJson.getString("body");

                if (inputType.equals("request") && inputHeader.equals("close-connection")) {
                    System.out.printf("Stop accepting from client %s%n", this.clientSocket.getInetAddress().getHostAddress());
                    break;
                }

                if (inputType.equals("request") && inputHeader.equals("new-rental")) {
                    // Send "new-rental" request
                    // to worker
                    //this.sendWorkerSocketOutput(input);
                }

                if (inputType.equals("request") && inputHeader.equals("update-availability")) {
                    // TODO: Choose worker to forward request
                    //  based on hash function on rental

                    //this.sendWorkerSocketOutput(input);
                }

                if (inputType.equals("request") && inputHeader.equals("show-bookings")) {
                    // TODO: Return result with MapReduce

                    // this.sendWorkerSocketOutput(input);
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