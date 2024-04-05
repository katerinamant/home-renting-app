package com.homerentals.backend;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.homerentals.domain.Rental;

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
		Worker worker = null;
		ServerSocket workerSocket = null;

		try {
			// Worker is listening on port 1000
			workerSocket = new ServerSocket(5000, 10);
			workerSocket.setReuseAddress(true);

			// Accept Master connection
			Socket masterSocket = workerSocket.accept();

			worker = new Worker(workerSocket, masterSocket);

			// Displaying that master server connected
			// to worker
			System.out.println("> Master connected: " + masterSocket.getInetAddress().getHostAddress());

			String input;
			while (true) {
				input = worker.readMasterSocketInput();
				if (input == null) {
					System.out.println("WORKER MAIN: Error reading Master Socket input");
					continue;
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

		} finally {
			if (workerSocket != null) {
				try {
					workerSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class RequestHandler implements Runnable {
		private final ArrayList<Rental> rentals;
		private final JSONObject requestJson;

		public RequestHandler(ArrayList<Rental> rentals, JSONObject requestJson) {
			this.rentals = rentals;
			this.requestJson = requestJson;
		}

		private Rental jsonToRentalObject(String input) {
			try {
				// Create Rental object from JSON
				JSONObject jsonObject = new JSONObject(input);
				String roomName = jsonObject.getString("roomName");
				String area = jsonObject.getString("area");
				double pricePerNight = jsonObject.getDouble("pricePerNight");
				int numOfPersons = jsonObject.getInt("numOfPersons");
				int numOfReviews = jsonObject.getInt("numOfReviews");
				int sumOfReviews = jsonObject.getInt("sumOfReviews");
				String startDate = jsonObject.getString("startDate");
				String endDate = jsonObject.getString("endDate");
				String imagePath = jsonObject.getString("imagePath");
				Rental rental = new Rental(roomName, area, pricePerNight, numOfPersons, numOfReviews, sumOfReviews, startDate, endDate, imagePath);
				//              System.out.println(rental.getRoomName());
				//			    System.out.println(rental.getStars());

				return rental;

			} catch (JSONException e) {
				// String is not valid JSON object
				System.out.println("REQUEST HANDLER: Error creating Rental object from JSON: " + e);
				return null;
			}
		}

		@Override
		public void run() {
			// Handle JSON input
			String inputType = this.requestJson.getString("type");
			String inputHeader = this.requestJson.getString("header");
			String inputBody = this.requestJson.getString("body");

			if (inputType.equals("request") && inputHeader.equals("new-rental")) {
				// Create Rental object from JSON
				Rental rental = this.jsonToRentalObject(inputBody);
				if (rental == null) {
					System.out.println("REQUEST HANDLER RUN: Error creating Rental object from JSON");
					return;
				}

				synchronized (this.rentals) {
					System.out.println("lock");
					System.out.println(this.rentals);
					this.rentals.add(rental);
				}
				System.out.println("done");
				System.out.println(this.rentals);
			}
		}
	}
}
