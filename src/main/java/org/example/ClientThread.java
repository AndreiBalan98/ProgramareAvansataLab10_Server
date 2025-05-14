package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * The ClientThread class is responsible for handling communication with a client socket.
 * Each client connection is handled in a separate thread.
 */
public class ClientThread implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running;

    /**
     * Creates a new ClientThread instance.
     *
     * @param socket The client socket
     * @param server The game server
     */
    public ClientThread(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Initialize input and output streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            running = true;
            String inputLine;

            // Welcome message
            out.println("Connected to Hex Game Server. Type 'exit' to disconnect, 'stop' to stop the server.");

            // Process client commands
            while (running && (inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);

                // Handle client disconnection
                if (inputLine.equalsIgnoreCase("exit")) {
                    out.println("Goodbye!");
                    break;
                }

                // Process the command and send response
                String response = server.processCommand(inputLine);
                out.println(response);

                // If server was stopped, exit the loop
                if (response.equals("Server stopped")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error in client thread: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Stops the client thread and closes all resources.
     */
    public void stop() {
        running = false;

        try {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
            }

            // Remove this client from the server's list
            server.removeClient(this);
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }
}