package com.homerentals.backend;

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
    // TODO: Replace System.out.println() with logger in log file.

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
            System.out.println("WORKER: Error reading Master Socket input: " + e);
            return null;
        }
    }

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java Worker <port>");
			System.exit(1);
		}

		Worker worker = null;
		int port = Integer.parseInt(args[0]);

		try (ServerSocket workerSocket = new ServerSocket(port, 10);){
			workerSocket.setReuseAddress(true);

            // Accept Master connection
            Socket masterSocket = workerSocket.accept();

            worker = new Worker(workerSocket, masterSocket);

            System.out.println("> Master connected: " + masterSocket.getInetAddress().getHostAddress());

            String input;
            while (true) {
                input = worker.readMasterSocketInput();
                if (input == null) {
                    System.out.println("WORKER MAIN: Error reading Master Socket input");
                    break;
                }
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
            System.out.println("WORKER MAIN: Error: " + e);
            e.printStackTrace();

        } 
    }
}
