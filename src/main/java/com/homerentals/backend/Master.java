package com.homerentals.backend;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

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

        ServerSocket serverSocket = null;
		ArrayList<Socket> workerSockets = new ArrayList<>();

		try {
			// Server is listening on port 8080
			serverSocket = new ServerSocket(8080, 10);
			serverSocket.setReuseAddress(true);

			// Connect to workers
			for (Integer port : ports) {
				workerSockets.add(new Socket("localhost", port));
			}

			// Handle client requests
			while (true) {
				Socket clientSocket = serverSocket.accept();

				// Displaying that new client is connected
				// to server
				System.out.println("> New client connected: " + clientSocket.getInetAddress().getHostAddress());

				// Create a new thread object
				// to handle this client separately
				ClientHandler clientThread = new ClientHandler(clientSocket, workerSockets.get(0));
				new Thread(clientThread).start();
			}

		} catch (IOException | RuntimeException e) {
			System.out.println("MASTER MAIN: IO Error: " + e);
			e.printStackTrace();

		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
					for (Socket workerSocket : workerSockets) {
						workerSocket.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class ClientHandler implements Runnable {
		private final Socket clientSocket;
		private final Socket workerSocket;
		private DataOutputStream workerSocketOut = null;
		private DataInputStream clientSocketIn = null;

		public ClientHandler(Socket clientSocket, Socket workerSocket) throws IOException {
			this.clientSocket = clientSocket;
			this.workerSocket = workerSocket;
			try {
				this.workerSocketOut = new DataOutputStream(this.workerSocket.getOutputStream());
				this.clientSocketIn = new DataInputStream(this.clientSocket.getInputStream());

			} catch (IOException e) {
				System.out.println("CLIENT HANDLER: Error setting up streams: " + e);
				throw e;
			}
		}

		private String readClientSocketInput() {
			try {
				return clientSocketIn.readUTF();

			} catch (IOException e) {
				System.out.println("CLIENT HANDLER: Error reading Client Socket input: " + e);
				return null;
			}
		}

		private JSONObject createRequest(String header, String body) {
			JSONObject request = new JSONObject();
			request.put("type", "request");
			request.put("header", header);
			request.put("body", body);

			return request;
		}

		private void sendWorkerSocketOutput(String msg) throws IOException {
			try {
				this.workerSocketOut.writeUTF(msg);
				this.workerSocketOut.flush();

			} catch (IOException e) {
				System.out.println("CLIENT HANDLER: Error sending Worker Socket output: " + e);
				throw e;
			}
		}

		@Override
		public void run() {
			// Read data sent from client
			String input = null;
			try {
				while (true) {
					input = this.readClientSocketInput();
					if (input == null) {
						System.out.println("REQUEST HANDLER RUN: Error reading Client Socket input");
						continue;
					}
					System.out.println(input);

					// Handle JSON input
					JSONObject inputJson = new JSONObject(input);
					String inputType = inputJson.getString("type");
					String inputHeader = inputJson.getString("header");
					String inputBody = inputJson.getString("body");

					if (inputType.equals("request") && inputHeader.equals("close-connection")) {
						System.out.printf("Stop accepting from client %s%n", this.clientSocket.getInetAddress().getHostAddress());
						break;
					}

					if (inputType.equals("request") && inputHeader.equals("new-rental")) {
						// Send "new-rental" request
						// to worker
						this.sendWorkerSocketOutput(input);
					}
				}

			} catch (JSONException | IOException e) {
				System.out.println("REQUEST HANDLER RUN: Error: " + e);
				e.printStackTrace();

			} finally {
				try {
					System.out.println("Closing thread");
					clientSocketIn.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
