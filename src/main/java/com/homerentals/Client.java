package com.homerentals;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private JSONObject json = null;
    private Socket requestSocket = null;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    public JSONObject getJson() {
        return json;
    }

    public Socket getRequestSocket() { return this.requestSocket; }

    public void setRequestSocket(Socket requestSocket) {
        this.requestSocket = requestSocket;
        try {
            this.out = new DataOutputStream(this.requestSocket.getOutputStream());
            this.in = new DataInputStream(this.requestSocket.getInputStream());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFile(String path) {
        // Read JSON file
        try {
            InputStream is = Files.newInputStream(Paths.get(path));
            String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);

            System.out.println(jsonTxt);
            this.json = new JSONObject(jsonTxt);

        } catch (IOException | JSONException e) {
            // Could not find file or
            // File is not valid JSON Object
            throw new RuntimeException(e);
        }
    }

    private void sendSocketOutput(String msg) {
        try {
            this.out.writeUTF(msg);
            this.out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void close() {
        try {
            this.in.close();
            this.out.close();
            this.requestSocket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        // Read JSON file
        try {
            client.readFile("src\\main\\java\\com\\homerentals\\util\\demo_rental.json");

            // Establish a connection
            Socket requestSocket = null;
                System.out.println("Connecting to server...");
                requestSocket = new Socket("localhost", 8080);
                client.setRequestSocket(requestSocket);

                // Write to socket
                System.out.println("Writing to server...");
                String msg = client.getJson().toString();
                client.sendSocketOutput(msg);

                // Read response from server
                // inputStream = new ObjectInputStream(socket.getInputStream());
                // String msg = (String) inputStream.readObject();

        } catch (RuntimeException | IOException e) {
            e.printStackTrace();

        } finally {
            if (client.getRequestSocket() != null) {
                try {
                    System.out.println("Closing down connection...");
                    client.close();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
