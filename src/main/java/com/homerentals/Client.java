package com.homerentals;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private JSONObject json = null;
    private Socket socket = null;

    public JSONObject getJson() {
        return json;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private void readFile(String path) {
        // Read JSON file
        try {
            InputStream is = Files.newInputStream(Paths.get(path));
            String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);

            System.out.println(jsonTxt);
            this.json = new JSONObject(jsonTxt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendSocketOutput(String msg) {
        try {
            PrintWriter writer = new PrintWriter(this.socket.getOutputStream(), true);
            writer.println(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        // Read JSON file
        try {
            client.readFile("\\util\\demo_rental.json");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        // Establish a connection
        Socket socket = null;
        try {
            System.out.println("Connecting to server...");
            socket = new Socket("localhost", 8080);
            client.setSocket(socket);

            // Write to socket
            System.out.println("Writing to server...");
            String msg = client.getJson().toString();
            client.sendSocketOutput(msg);

            // Read response from server
//            inputStream = new ObjectInputStream(socket.getInputStream());
//            String msg = (String) inputStream.readObject();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (socket != null) {
                try {
                    System.out.println("Closing down connection...");
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
