package com.homerentals.backend;

import com.homerentals.domain.Rental;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Worker {
    // TODO: Replace System.out.println() with logger in log file.
    protected final static ArrayList<Rental> rentals = new ArrayList<>();
    protected final static HashMap<Integer, Rental> idToRental = new HashMap<>();

    public static void writeToReducerSocket(MapResult results) throws IOException {
        try (Socket reducerSocket = new Socket("localhost", BackendUtils.REDUCER_PORT);
             ObjectOutputStream reducerSocketOutput = new ObjectOutputStream(reducerSocket.getOutputStream())
        ) {
            reducerSocketOutput.writeObject(results);
            reducerSocketOutput.flush();
        } catch (IOException e) {
            System.err.println("\n! Worker.writeToReducerSocket(): Failed to write to Reducer: " + BackendUtils.REDUCER_PORT);
            throw e;
        }
    }

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
            System.err.println("\n! Worker.main(): Error:\n" + e);
            e.printStackTrace();
        }
    }
}
