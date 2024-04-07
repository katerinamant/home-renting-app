package com.homerentals.backend;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class PortManager {
    private static final int START_PORT = 50000;
    private static final int MAX_PORT = 65535;
    private static final int MAX_ATTEMPTS = 1000; // Max attempts to find a free port

    public static int findAvailablePort(int basePort) {
        if (basePort < START_PORT || basePort > MAX_PORT) {
            throw new IllegalArgumentException("Base port out of valid range");
        }

        int currentPort = basePort;
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            if (isPortAvailable(currentPort)) {
                return currentPort;
            }
            currentPort++;
            if (currentPort > MAX_PORT) {
                currentPort = START_PORT;
            }
            attempts++;
        }

        throw new RuntimeException("Could not find an available port");
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java PortManager <number_of_ports> <output_file>");
            System.exit(1);
        }

        int numberOfPorts = Integer.parseInt(args[0]);
        String outputFile = args[1];

        List<Integer> reservedPorts = new ArrayList<>();

        int currentPort = START_PORT;

        try {
            // Attempt to reserve the specified number of ports
            for (int i = 0; i < numberOfPorts; i++) {
                try {
                    int reservedPort = findAvailablePort(currentPort);
                    reservedPorts.add(reservedPort);
                    currentPort = ++reservedPort;
                } catch (Exception e) {
                    System.err.printf("Could not reserve port %d/%d%n", i, numberOfPorts);
                }
            }

            // Write the reserved ports to the output file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                for (int port : reservedPorts) {
                    writer.write(Integer.toString(port));
                    writer.newLine();
                }
            }

            System.out.printf("Successfully reserved %d/%d ports.%n", reservedPorts.size(), numberOfPorts);
        } catch (IOException e) {
            System.err.printf("Error: %s%n", e.getMessage());
            System.exit(1);
        }
    }
}

