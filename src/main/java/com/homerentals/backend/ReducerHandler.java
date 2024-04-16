package com.homerentals.backend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ReducerHandler implements Runnable {
    protected final static Object syncObj = new Object();

    private final Socket reducerSocket;
    private final ObjectInputStream reducerSocketIn;

    ReducerHandler(Socket reducerSocket) throws IOException {
        this.reducerSocket = reducerSocket;
        try {
            this.reducerSocketIn = new ObjectInputStream(reducerSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("\n! ReducerHandler(): Error setting up streams:\n" + e);
            throw e;
        }
    }

    private MapResult readReducerSocketInput() {
        try {
            return (MapResult) this.reducerSocketIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("\n! ReducerHandler.readReducerSocketInput(): Error reading Reducer Socket input:\n" + e);
            return null;
        }
    }

    @Override
    public void run() {
        // Read object sent from reducer
        MapResult mapResult = null;
        while (true) {
            mapResult = this.readReducerSocketInput();
            if (mapResult == null) {
                System.err.println("\n! ReducerHandler.run(): Error reading mapResult from Reducer.");
                break;
            }
            System.out.println("\n> ReducerHandler.run(): Received result with mapId = " + mapResult.getMapId());

            // Put results in Server.mapReduceResults
            // and notify waiting ClientHandler threads
            synchronized (syncObj) {
                Server.mapReduceResults.put(mapResult.getMapId(), mapResult);
                syncObj.notify();
            }
        }
        try {
            System.out.println("\n> Closing thread...");
            this.reducerSocketIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
