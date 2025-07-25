/**
 * TestChat - Simple command-line client for testing the chat server
 * 
 * This is a basic text-based client that can be used to test the chat server
 * without needing the GUI. Useful for automated testing or command-line usage.
 * 
 * @author Chat Application
 * @version 1.0.0
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TestChat {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("=== Chat Client Test ===");
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Start thread to receive messages
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost.");
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();
            
            // Send username
            out.println(username);
            
            // Send messages
            System.out.println("Connected! Type your messages (or 'quit' to exit):");
            String input;
            while (!(input = scanner.nextLine()).equals("quit")) {
                out.println(input);
            }
            
            socket.close();
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        scanner.close();
    }
}