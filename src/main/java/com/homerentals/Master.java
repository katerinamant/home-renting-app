package com.homerentals;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Master {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket workerSocket = null;

        try {
            // Server is listening on port 8080
            serverSocket = new ServerSocket(8080, 10);
            serverSocket.setReuseAddress(true);

            // Connect to worker
            workerSocket = new Socket("localhost", 1000);

            // Handle client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Displaying that new client is connected
                // to server
                System.out.println("> New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread object
                // to handle this client separately
                ClientHandler clientThread = new ClientHandler(clientSocket, workerSocket);
                new Thread(clientThread).start();
            }

        } catch (IOException e) {
            System.err.println("Server IO Error !\n");
            e.printStackTrace();

        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    workerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final Socket workerSocket;
        private DataOutputStream workerSocketOut = null;
        private DataInputStream clientSocketIn = null;

        public ClientHandler(Socket clientSocket, Socket workerSocket) {
            this.clientSocket = clientSocket;
            this.workerSocket = workerSocket;
            try {
                this.workerSocketOut = new DataOutputStream(this.workerSocket.getOutputStream());
                this.clientSocketIn = new DataInputStream(this.clientSocket.getInputStream());

            } catch (IOException e) {
                String msg = String.format("\tClient %s | Error creating streams:%n", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg, e.getCause());
            }
        }

        private String readClientSocketInput() {
            try {
                return clientSocketIn.readUTF();

            } catch (IOException e) {
                String msg = String.format("\tClient %s | Error reading input%n", this.clientSocket.getInetAddress().getHostAddress());
                throw new RuntimeException(msg, e.getCause());
            }
        }

        private JSONObject createRequest(String header, String body) {
            JSONObject request = new JSONObject();
            request.put("type", "request");
            request.put("header", header);
            request.put("body", body);

            return request;
        }

        private void sendWorkerSocketOutput(String msg) {
            try {
                this.workerSocketOut.writeUTF(msg);
                this.workerSocketOut.flush();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            // Read data sent from client
            String input = null;
            try {
                while (true) {
                    input = this.readClientSocketInput();
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
                        this.sendWorkerSocketOutput(input);
                    }
                }

            } catch (RuntimeException e) {
                String title = String.format("ERROR Client %s%n", this.clientSocket.getInetAddress().getHostAddress());
                System.err.println(title);
                e.printStackTrace();

            } finally {
                try {
                    // System.out.println("Closing thread");
                    clientSocketIn.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
