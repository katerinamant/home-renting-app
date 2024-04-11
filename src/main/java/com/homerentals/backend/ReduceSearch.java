package com.homerentals.backend;

import com.homerentals.domain.Rental;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ReduceSearch {
    private static final HashMap<Integer, ArrayList<ArrayList<Rental>>> rentalsToReduce = new HashMap<>();

    private static Object readWorkerSocketInput(ObjectInputStream in) {
        try {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Reducer.readWorkerSocketInput(): " + e.getMessage());
            return null;
        }
    }

    private static void writeToServerSocket(ObjectOutputStream out, Object output) {
        try {
            out.writeObject(output);
            out.flush();
        } catch (IOException e) {
            System.err.println("Reducer.writeToServerSocket(): " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ReduceSearch <worker-number>");
            System.exit(1);
        }

        int numOfWorkers = Integer.parseInt(args[0]);

        // Connect to server
        try (Socket serverSocket = new Socket("localhost", BackendUtils.SERVER_PORT);
             ObjectOutputStream serverSocketOutput = new ObjectOutputStream(serverSocket.getOutputStream())
        ) {
            // Set up reducer socket
            try (ServerSocket reducerSocket = new ServerSocket(BackendUtils.REDUCER_PORT)) {
                reducerSocket.setReuseAddress(true);
                while (true) {
                    // Accept connection from worker
                    Socket workerSocket = reducerSocket.accept();
                    System.out.println("> New worker connected " + workerSocket.getRemoteSocketAddress());

                    // Get message from worker
                    ObjectInputStream workerSocketInput = new ObjectInputStream(workerSocket.getInputStream());
                    MapResult workerInput = (MapResult) readWorkerSocketInput(workerSocketInput);
                    if (workerInput == null) {
                        System.err.println("ReduceSearch.main(): Error reading worker socket input.");
                        break;
                    }

                    // Parse message
                    int mapId = workerInput.getMapId();
                    ArrayList<Rental> workerRentals = workerInput.getRentals();
                    System.out.printf("> Received message from worker with mapId: %d with rentals: %s%n", mapId, workerRentals);

                    // Save rentals based on mapId
                    if (!rentalsToReduce.containsKey(mapId)) {
                        rentalsToReduce.put(mapId, new ArrayList<>());
                    }
                    rentalsToReduce.get(mapId).add(workerRentals);
                    System.out.printf("> MapReduce for #%d at %d/%d messages.%n", mapId, rentalsToReduce.get(mapId).size(), numOfWorkers);

                    // Reduce values when all workers have sent their results
                    if (rentalsToReduce.get(mapId).size() == numOfWorkers) {
                        System.out.printf("> Reducing for #%d.%n", mapId);
                        ArrayList<Rental> reducedRentals = reduce(mapId);
                        rentalsToReduce.remove(mapId);

                        // Send results to server
                        System.out.println("> Sending results to server: " + reducedRentals);
                        MapResult mapResult = new MapResult(mapId, reducedRentals);
                        writeToServerSocket(serverSocketOutput, mapResult);
                    }
                }
            } catch (IOException e) {
                System.err.println("ReduceSearch.main(): Could not set up Reducer ServerSocket: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("ReduceSearch.main(): Could not connect to Server: " + e.getMessage());
        }
    }

    public static ArrayList<Rental> reduce(int mapId) {
        ArrayList<ArrayList<Rental>> unreducedRentals = rentalsToReduce.get(mapId);

        HashSet<Rental> reduced = new HashSet<>();
        for (ArrayList<Rental> rentals : unreducedRentals) {
            reduced.addAll(rentals);
        }

        return new ArrayList<>(reduced);
    }
}
