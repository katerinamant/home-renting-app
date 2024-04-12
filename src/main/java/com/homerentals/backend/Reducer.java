package com.homerentals.backend;

import com.homerentals.domain.Booking;
import com.homerentals.domain.Rental;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Reducer {
    private static final HashMap<Integer, ArrayList<MapResult>> resultsToReduce = new HashMap<>();

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
                    System.out.printf("> Received message from worker with mapId: %d", mapId);

                    // Save MapResults based on mapId
                    if (!resultsToReduce.containsKey(mapId)) {
                        resultsToReduce.put(mapId, new ArrayList<>());
                    }
                    resultsToReduce.get(mapId).add(workerInput);
                    System.out.printf("> MapReduce for #%d at %d/%d messages.%n", mapId, resultsToReduce.get(mapId).size(), numOfWorkers);

                    // Reduce values when all workers have sent their results
                    if (resultsToReduce.get(mapId).size() == numOfWorkers) {
                        System.out.printf("> Reducing for #%d.%n", mapId);
                        MapResult reducedResults;

                        // Check what type of reduction you need to do
                        if (workerInput.containsRentals()) {
                            ArrayList<Rental> reducedRentals = reduceRentals(mapId);
                            reducedResults = new MapResult(mapId, reducedRentals, null);
                        } else {
                            ArrayList<BookingsByLocation> reducedBookingsByLocation = reduceBookingsByLocation(mapId);
                            reducedResults = new MapResult(mapId, null, reducedBookingsByLocation);
                        }

                        resultsToReduce.remove(mapId);

                        // Send results to server
                        System.out.println("> Sending results to server for #" + mapId);
                        writeToServerSocket(serverSocketOutput, reducedResults);
                    }

                }
            } catch (IOException e) {
                System.err.println("ReduceSearch.main(): Could not set up Reducer ServerSocket: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("ReduceSearch.main(): Could not connect to Server: " + e.getMessage());
        }
    }

    public static ArrayList<Rental> reduceRentals(int mapId) {
        ArrayList<MapResult> resultsList = resultsToReduce.get(mapId);

        HashSet<Rental> reduced = new HashSet<>();
        for (MapResult result : resultsList) {
            reduced.addAll(result.getRentals());
        }

        return new ArrayList<>(reduced);
    }

    public static ArrayList<BookingsByLocation> reduceBookingsByLocation(int mapId) {
        ArrayList<MapResult> resultsList = resultsToReduce.get(mapId);

        // Perform reduction based on unique Booking IDs
        HashMap<String, BookingsByLocation> reduced = new HashMap<>();
        for (MapResult result : resultsList) {
            for (BookingsByLocation bookingsByLocation : result.getBookingsByLocation()) {
                String location = bookingsByLocation.getLocation();
                if (!reduced.containsKey(location)) {
                    // Create a new entry in the map by using
                    // the first worker's object
                    reduced.put(location, bookingsByLocation);
                } else {
                    // Add all the booking ids of this worker
                    // to the already existing map entry
                    reduced.get(location).addAll(bookingsByLocation.getBookingIds());
                }
            }
        }

        return new ArrayList<>(reduced.values());
    }
}
