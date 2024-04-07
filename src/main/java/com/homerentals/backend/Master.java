package com.homerentals.backend;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Master {
    // TODO: Replace System.out.println() with logger in log file.

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port_list_file>");
            System.exit(1);
        }

        // Get worker ports from file
        String filePath = args[0];
        ArrayList<Integer> ports = new ArrayList<>();

        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            List<String> lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
            for (String line : lines) {
                ports.add(Integer.parseInt(line.trim()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ServerSocket serverSocket = new ServerSocket(8080, 10)) {
            serverSocket.setReuseAddress(true);

            // Handle client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("> New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket, ports);
                new Thread(clientThread).start();
            }

        } catch (IOException | RuntimeException e) {
            System.out.println("MASTER MAIN: IO Error: " + e);
            e.printStackTrace();
        }
    }
}
