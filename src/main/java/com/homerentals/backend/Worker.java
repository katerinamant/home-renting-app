package com.homerentals.backend;

import com.homerentals.domain.Rental;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Worker {
    // TODO: Replace System.out.println() with logger in log file.
    protected final static ArrayList<Rental> rentals = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Worker <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket workerSocket = new ServerSocket(port, 10)) {
            workerSocket.setReuseAddress(true);

            // Accept Master connection
            while (true) {
                Socket masterSocket = workerSocket.accept();
                RequestHandler requestThread = new RequestHandler(masterSocket);
                new Thread(requestThread).start();
            }
        } catch (IOException e) {
            System.out.println("WORKER MAIN: Error: " + e);
            e.printStackTrace();
        }
    }
}
