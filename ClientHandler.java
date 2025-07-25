import java.io.*;
import java.net.*;

/**
 * ClientHandler - Handles individual client connections in separate threads
 * 
 * Each instance of this class runs in its own thread and manages communication
 * with a single client. It receives messages from the client and forwards them
 * to the server for broadcasting to other clients.
 * 
 * Features:
 * - Handles individual client communication
 * - Manages username assignment
 * - Processes incoming messages
 * - Handles client disconnection gracefully
 * - Thread-safe message sending
 * 
 * @author Chat Application
 * @version 1.0.0
 */
public class ClientHandler implements Runnable {
    
    private Socket clientSocket;
    private ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private volatile boolean isConnected = false;
    
    /**
     * Constructor for ClientHandler
     * 
     * @param socket The client socket connection
     * @param server Reference to the main chat server
     */
    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.username = "";
        
        try {
            // Set up input and output streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            isConnected = true;
            
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
            disconnect();
        }
    }
    
    /**
     * Main thread execution method - handles client communication
     */
    @Override
    public void run() {
        try {
            // Welcome message and username setup
            sendMessage("🎉 Welcome to the Chat Server!");
            sendMessage("Please enter your username:");
            
            // Get username from client
            String inputUsername = in.readLine();
            if (inputUsername != null && !inputUsername.trim().isEmpty()) {
                username = inputUsername.trim();
                
                // Validate username length
                if (username.length() > 20) {
                    username = username.substring(0, 20);
                }
                
                // Remove any special characters that might cause issues
                username = username.replaceAll("[^a-zA-Z0-9_-]", "");
                
                if (username.isEmpty()) {
                    username = "User" + System.currentTimeMillis() % 1000;
                }
                
                sendMessage("✅ Welcome to the chat, " + username + "!");
                sendMessage("💬 You can start chatting now. Type your messages below:");
                
                // Notify other clients about the new user
                server.broadcastMessage("📢 " + username + " joined the chat", this);
                
                System.out.println("User '" + username + "' connected from " + 
                    clientSocket.getInetAddress());
                
            } else {
                sendMessage("❌ Invalid username. Disconnecting...");
                disconnect();
                return;
            }
            
            // Main message receiving loop
            String inputMessage;
            while (isConnected && (inputMessage = in.readLine()) != null) {
                
                // Skip empty messages
                if (inputMessage.trim().isEmpty()) {
                    continue;
                }
                
                // Handle special commands
                if (inputMessage.startsWith("/")) {
                    handleCommand(inputMessage);
                } else {
                    // Regular chat message - broadcast to all other clients
                    String formattedMessage = formatMessage(inputMessage);
                    server.broadcastMessage(formattedMessage, this);
                    
                    // Log the message on server
                    System.out.println("Message from " + username + ": " + inputMessage);
                }
            }
            
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }
    
    /**
     * Handles special chat commands
     * 
     * @param command The command string starting with '/'
     */
    private void handleCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "/help":
                sendMessage("📚 Available commands:");
                sendMessage("  /help - Show this help message");
                sendMessage("  /users - List all connected users");
                sendMessage("  /quit - Leave the chat");
                sendMessage("  /time - Show current server time");
                break;
                
            case "/users":
                sendMessage("👥 Connected users (" + server.getClientCount() + "):");
                for (String user : server.getConnectedUsers()) {
                    sendMessage("  • " + user);
                }
                break;
                
            case "/quit":
                sendMessage("👋 Goodbye " + username + "!");
                disconnect();
                break;
                
            case "/time":
                sendMessage("🕐 Server time: " + new java.util.Date());
                break;
                
            default:
                sendMessage("❓ Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }
    
    /**
     * Formats a message with username and timestamp
     * 
     * @param message The raw message from the client
     * @return Formatted message string
     */
    private String formatMessage(String message) {
        // Add timestamp (optional - can be removed if not desired)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String timestamp = sdf.format(new java.util.Date());
        
        return "[" + timestamp + "] " + username + ": " + message;
    }
    
    /**
     * Sends a message to this client
     * 
     * @param message The message to send
     */
    public synchronized void sendMessage(String message) {
        if (isConnected && out != null) {
            out.println(message);
            
            // Check if the message was sent successfully
            if (out.checkError()) {
                System.err.println("Error sending message to " + username);
                disconnect();
            }
        }
    }
    
    /**
     * Gracefully disconnects the client
     */
    public void disconnect() {
        if (!isConnected) return;
        
        isConnected = false;
        
        try {
            // Close streams
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
        
        // Remove this client from the server
        server.removeClient(this);
        
        System.out.println("Client " + username + " disconnected");
    }
    
    /**
     * Returns the client's username
     * 
     * @return The username string
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Checks if the client is still connected
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && clientSocket.isConnected() && !clientSocket.isClosed();
    }
    
    /**
     * Returns the client's IP address
     * 
     * @return IP address as string
     */
    public String getClientAddress() {
        if (clientSocket != null) {
            return clientSocket.getInetAddress().getHostAddress();
        }
        return "Unknown";
    }
    
    /**
     * Returns string representation of this client handler
     * 
     * @return String with username and IP address
     */
    @Override
    public String toString() {
        return "ClientHandler{" +
                "username='" + username + '\'' +
                ", address=" + getClientAddress() +
                ", connected=" + isConnected +
                '}';
    }
}