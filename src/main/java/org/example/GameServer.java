package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The GameServer class is responsible for managing the Hex game and mediating between players.
 * It creates a server socket, accepts client connections, and handles them in separate threads.
 */
public class GameServer {
    private ServerSocket serverSocket;
    private final int port;
    private boolean running;
    private final ExecutorService pool;
    private final List<ClientThread> clients = new ArrayList<>();

    /**
     * Creates a new GameServer instance.
     *
     * @param port The port number on which the server will listen for connections
     */
    public GameServer(int port) {
        this.port = port;
        // Create a thread pool to handle client connections
        this.pool = Executors.newFixedThreadPool(10); // Maximum 10 concurrent clients
    }

    /**
     * Starts the server and begins accepting client connections.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    // Create and start a new thread for the client
                    ClientThread clientThread = new ClientThread(clientSocket, this);
                    clients.add(clientThread);
                    pool.execute(clientThread);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Stops the server and all client threads.
     */
    public void stop() {
        running = false;

        // Close all client connections
        for (ClientThread client : clients) {
            client.stop();
        }
        clients.clear();

        // Shutdown the thread pool
        pool.shutdown();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println("Server stopped");
    }

    /**
     * Removes a client from the server's list of clients.
     *
     * @param client The client to remove
     */
    public void removeClient(ClientThread client) {
        clients.remove(client);
    }

    /**
     * Processes a command from a client.
     *
     * @param command The command to process
     * @return The response to send back to the client
     */
    public String processCommand(String command) {
        if (command.equalsIgnoreCase("stop")) {
            // Stop the server when receiving the "stop" command
            new Thread(this::stop).start();
            return "Server stopped";
        }

        // For now, just acknowledge receipt of the command
        return "Server received the request: " + command;
    }

    /**
     * The main method to start the server.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        int port = 8099; // Default port

        // Check if port is specified in command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 8099.");
            }
        }

        GameServer server = new GameServer(port);
        server.start();
    }
}