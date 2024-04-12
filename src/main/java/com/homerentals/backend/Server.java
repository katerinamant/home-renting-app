package com.homerentals.backend;

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server {
    // TODO: Replace System.out.println() with logger in log file.
    protected final static ArrayList<Integer> ports = new ArrayList<>();
    protected final static HashMap<Integer, MapResult> mapReduceResults = new HashMap<>();

    private static int numberOfRentals;
    private static int mapId;
    private static int bookingId;

    public static int getNextRentalId() {
        return numberOfRentals++;
    }

    public static int getNextMapId() {
        return mapId++;
    }

    public static String getNextBookingId() {
        return String.valueOf(bookingId++);
    }

    protected static int hash(int rentalId) {
        int numOfWorkers = ports.size();
        float A = 0.357840f;
        return (int) Math.floor(numOfWorkers * ((rentalId * A) % 1));
    }

    private static void writeToWorkerSocket(String msg, int port) throws IOException {
        try (Socket workerSocket = new Socket("localhost", port);
             DataOutputStream workerSocketOutput = new DataOutputStream(workerSocket.getOutputStream())
        ) {
            workerSocketOutput.writeUTF(msg);
            workerSocketOutput.flush();
        } catch (IOException e) {
            System.err.println("Server.writeToWorkerSocket(): Failed to write to Worker:" + port);
            throw e;
        }
    }

    protected static void sendMessageToWorker(String msg, int port) {
        try {
            writeToWorkerSocket(msg, port);
        } catch (IOException e) {
            System.err.println("Server.sendMessageToWorker(): Failed to write to Worker:" + port);
        }
    }

    protected static void sendMessageToWorkers(String msg, ArrayList<Integer> ports) {
        for (int p : ports) {
            sendMessageToWorker(msg, p);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port_list_file>");
            System.exit(1);
        }

        // Get worker ports from file
        String filePath = args[0];
        ports.clear();

        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            List<String> lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
            for (String line : lines) {
                ports.add(Integer.parseInt(line.trim()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ServerSocket serverSocket = new ServerSocket(BackendUtils.SERVER_PORT, 10)) {
            serverSocket.setReuseAddress(true);

            // Start thread that listens to Reducer
            Socket reducerSocket = serverSocket.accept();
            System.out.printf("> Reducer:%s connected.%n", reducerSocket.getRemoteSocketAddress());
            ReducerHandler reducerHandler = new ReducerHandler(reducerSocket);
            new Thread(reducerHandler).start();

            // Handle client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("> New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket);
                new Thread(clientThread).start();
            }

        } catch (IOException | RuntimeException e) {
            System.out.println("MASTER MAIN: IO Error: " + e);
            e.printStackTrace();
        }
    }
}
