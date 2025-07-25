import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ChatServer - Multi-threaded chat server using Java sockets
 * 
 * This server can handle multiple clients simultaneously using multithreading.
 * Each client connection is managed by a separate ClientHandler thread.
 * The server broadcasts messages from one client to all other connected clients.
 * 
 * Features:
 * - Accepts multiple client connections
 * - Broadcasts messages to all connected clients
 * - Handles client join/leave notifications
 * - Graceful shutdown and cleanup
 * 
 * @author Chat Application
 * @version 1.0.0
 */
public class ChatServer {
    
    // Server configuration
    private static final int PORT = 12345;
    private static final int MAX_CLIENTS = 50;
    
    // Server socket and client management
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private ExecutorService threadPool;
    
    // Thread-safe collection to store all connected clients
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    
    /**
     * Constructor initializes the thread pool for handling clients
     */
    public ChatServer() {
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }
    
    /**
     * Main method to start the chat server
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Add shutdown hook for graceful server termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.shutdown();
        }));
        
        server.start();
    }
    
    /**
     * Starts the server and begins accepting client connections
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            
            System.out.println("=================================");
            System.out.println("    Chat Server Started");
            System.out.println("=================================");
            System.out.println("Server listening on port: " + PORT);
            System.out.println("Maximum clients: " + MAX_CLIENTS);
            System.out.println("Waiting for clients to connect...");
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("=================================");
            
            // Main server loop - accepts new client connections
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Check if we've reached maximum client limit
                    if (clients.size() >= MAX_CLIENTS) {
                        System.out.println("Maximum clients reached. Rejecting connection from: " 
                            + clientSocket.getInetAddress());
                        clientSocket.close();
                        continue;
                    }
                    
                    // Create and start new client handler thread
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    threadPool.execute(clientHandler);
                    
                    System.out.println("New client connected from: " + clientSocket.getInetAddress() 
                        + " | Total clients: " + clients.size());
                    
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Server startup error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * Broadcasts a message to all connected clients except the sender
     * 
     * @param message The message to broadcast
     * @param sender The client that sent the message (to exclude from broadcast)
     */
    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("Broadcasting: " + message);
        
        // Create a copy of clients to avoid ConcurrentModificationException
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);
        
        for (ClientHandler client : clientsCopy) {
            if (client != sender && client.isConnected()) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                    // Remove disconnected client
                    removeClient(client);
                }
            }
        }
    }
    
    /**
     * Broadcasts a message to all connected clients (including sender)
     * 
     * @param message The message to broadcast to everyone
     */
    public synchronized void broadcastToAll(String message) {
        System.out.println("Broadcasting to all: " + message);
        
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);
        
        for (ClientHandler client : clientsCopy) {
            if (client.isConnected()) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Error sending message to client: " + e.getMessage());
                    removeClient(client);
                }
            }
        }
    }
    
    /**
     * Removes a client from the server's client list
     * 
     * @param clientHandler The client to remove
     */
    public synchronized void removeClient(ClientHandler clientHandler) {
        if (clients.remove(clientHandler)) {
            System.out.println("Client removed. Total clients: " + clients.size());
            
            // Notify other clients that someone left
            String username = clientHandler.getUsername();
            if (username != null && !username.isEmpty()) {
                broadcastToAll("📢 " + username + " left the chat");
            }
        }
    }
    
    /**
     * Returns the current number of connected clients
     * 
     * @return Number of connected clients
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Returns a list of all connected usernames
     * 
     * @return List of usernames
     */
    public synchronized List<String> getConnectedUsers() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            String username = client.getUsername();
            if (username != null && !username.isEmpty()) {
                usernames.add(username);
            }
        }
        return usernames;
    }
    
    /**
     * Gracefully shuts down the server and all client connections
     */
    public void shutdown() {
        if (!isRunning) return;
        
        isRunning = false;
        
        System.out.println("Shutting down server...");
        
        // Notify all clients that server is shutting down
        broadcastToAll("🚨 Server is shutting down. Goodbye!");
        
        // Close all client connections
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);
        for (ClientHandler client : clientsCopy) {
            client.disconnect();
        }
        clients.clear();
        
        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        System.out.println("Server shutdown complete.");
    }
    
    /**
     * Checks if the server is currently running
     * 
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
