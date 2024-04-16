package com.homerentals.backend;

import com.homerentals.dao.GuestAccountDAO;
import com.homerentals.domain.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server {
    // TODO: Replace System.out.println() with logger in log file.
    protected final static ArrayList<Integer> ports = new ArrayList<>();
    protected final static HashMap<Integer, MapResult> mapReduceResults = new HashMap<>();
    private final static GuestAccountDAO guestAccountDAO = new GuestAccountDAO();

    private static int numberOfRentals;
    private static int numberOfRequests;
    private static int mapId;
    private static int bookingId;

    public static int getNextRentalId() {
        return numberOfRentals++;
    }

    public static int getNextRequestId() {
        return numberOfRequests++;
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

    protected static ArrayList<BookingReference> getGuestBookings(String email) {
        GuestAccount guestAccount = guestAccountDAO.find(email);
        if (guestAccount == null) {
            return null;
        }
        return guestAccount.getUnratedBookings();
    }

    protected static void addBookingToGuest(String email, String bookingId, int rentalId, String rentalName, String rentalLocation, LocalDate startDate, LocalDate endDate) {
        GuestAccount guestAccount = guestAccountDAO.find(email);
        if (guestAccount == null) {
            return;
        }
        guestAccount.addBooking(bookingId, rentalId, rentalName, rentalLocation, startDate, endDate);
    }

    protected static void rateGuestsBooking(String email, String bookingId) {
        GuestAccount guestAccount = guestAccountDAO.find(email);
        if (guestAccount == null) {
            return;
        }
        guestAccount.rateBooking(bookingId);
    }

    protected static String sendMessageToWorkerAndWaitForResponse(String msg, int port) {
        try (Socket workerSocket = new Socket("localhost", port);
             DataOutputStream workerSocketOutput = new DataOutputStream(workerSocket.getOutputStream());
             DataInputStream workerSocketInput = new DataInputStream(workerSocket.getInputStream())
        ) {
            workerSocketOutput.writeUTF(msg);
            workerSocketOutput.flush();

            // Receive response
            return workerSocketInput.readUTF();
        } catch (IOException e) {
            System.err.printf("\n! Server.writeToWorkerSocketAndWaitForResponse(): Failed to set up Socket to Worker: %d%n%s%n", port, e);
            return null;
        }
    }

    private static void writeToWorkerSocket(String msg, int port) throws IOException {
        try (Socket workerSocket = new Socket("localhost", port);
             DataOutputStream workerSocketOutput = new DataOutputStream(workerSocket.getOutputStream())
        ) {
            workerSocketOutput.writeUTF(msg);
            workerSocketOutput.flush();
        } catch (IOException e) {
            System.err.println("\n! Server.writeToWorkerSocket(): Failed to write to Worker: " + port);
            throw e;
        }
    }

    protected static void sendMessageToWorker(String msg, int port) {
        try {
            writeToWorkerSocket(msg, port);
        } catch (IOException e) {
            System.err.println("\n! Server.sendMessageToWorker(): Failed to write to Worker: " + port);
        }
    }

    protected static void sendMessageToWorkers(String msg, ArrayList<Integer> ports) {
        for (int p : ports) {
            sendMessageToWorker(msg, p);
        }
    }

    private static void setUpRental(String path, int id, String bookingStartDate, String bookingEndDate) throws InterruptedException {
        JSONObject rentalJson = BackendUtils.readFile(path);
        if (rentalJson != null) {
            rentalJson.put(BackendUtils.BODY_FIELD_REQUEST_ID, -1);
            BackendUtils.executeNewRentalRequest(rentalJson, Requests.NEW_RENTAL.name());
        } else {
            return;
        }
        Thread.sleep(1000);

        // Make available for 2023
        JSONObject body = new JSONObject();
        body.put(BackendUtils.BODY_FIELD_RENTAL_ID, id);
        body.put(BackendUtils.BODY_FIELD_START_DATE, "01/01/2023");
        body.put(BackendUtils.BODY_FIELD_END_DATE, "31/12/2023");
        body.put(BackendUtils.BODY_FIELD_REQUEST_ID, -1);
        JSONObject request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), body.toString());
        BackendUtils.executeUpdateAvailability(request.toString(), body);
        Thread.sleep(1000);
        // Make available for 2024
        body = new JSONObject();
        body.put(BackendUtils.BODY_FIELD_RENTAL_ID, id);
        body.put(BackendUtils.BODY_FIELD_START_DATE, "01/01/2024");
        body.put(BackendUtils.BODY_FIELD_END_DATE, "31/12/2024");
        body.put(BackendUtils.BODY_FIELD_REQUEST_ID, -1);
        request = BackendUtils.createRequest(Requests.UPDATE_AVAILABILITY.name(), body.toString());
        BackendUtils.executeUpdateAvailability(request.toString(), body);
        Thread.sleep(1000);

        // Add new Booking
        body = new JSONObject();
        body.put(BackendUtils.BODY_FIELD_RENTAL_ID, id);
        body.put(BackendUtils.BODY_FIELD_START_DATE, bookingStartDate);
        body.put(BackendUtils.BODY_FIELD_END_DATE, bookingEndDate);
        body.put(BackendUtils.BODY_FIELD_REQUEST_ID, -1);
        body.put(BackendUtils.BODY_FIELD_GUEST_EMAIL, "guest@example.com");
        BackendUtils.executeNewBookingRequest(body, Requests.NEW_BOOKING.name());
        Thread.sleep(1000);
    }

    private static void setUp() throws IOException, InterruptedException {
        // Add guest account
        Email email = new Email("guest@example.com");
        Password password = new Password("guest");
        PhoneNumber phoneNumber = new PhoneNumber("123456789");
        GuestAccount guestAccount = new GuestAccount(email, password, "Guest", "Guest", phoneNumber);
        guestAccountDAO.save(guestAccount);

        // Add cozy_rental_crete.json
        setUpRental(BackendUtils.inputsPath + "cozy_rental_crete.json", 0, "01/01/2023", "31/12/2023");

        // Add lux_rental_crete.json
        setUpRental(BackendUtils.inputsPath + "lux_rental_crete.json", 1, "01/10/2023", "02/10/2023");

        // Add best_spitarwn_zante.json
        setUpRental(BackendUtils.inputsPath + "best_spitarwn_zante.json", 2, "01/10/2023", "28/12/2023");
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
            System.out.printf("\n> Reducer:%s connected.%n", reducerSocket.getRemoteSocketAddress());
            ReducerHandler reducerHandler = new ReducerHandler(reducerSocket);
            new Thread(reducerHandler).start();

            Server.setUp();

            // Handle client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n> New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket);
                new Thread(clientThread).start();
            }
        } catch (IOException | RuntimeException | InterruptedException e) {
            System.err.println("\n! Server.main(0): Error:\n" + e);
            e.printStackTrace();
        }
    }
}
