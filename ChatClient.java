import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * ChatClient - GUI-based chat client application
 * 
 * This client connects to the ChatServer and provides a graphical user interface
 * for users to send and receive messages in real-time. It uses Swing for the GUI
 * and runs a separate thread to receive messages from the server.
 * 
 * Features:
 * - Swing-based GUI with message display and input
 * - Real-time message receiving in separate thread
 * - Username input and validation
 * - Proper connection handling and cleanup
 * - Scroll-to-bottom for new messages
 * 
 * @author Chat Application
 * @version 1.0.0
 */
public class ChatClient extends JFrame {
    
    // Server connection details
    private static final String SERVER_HOST = "localhost";  // Change to server IP if needed
    private static final int SERVER_PORT = 12345;
    
    // Network components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;
    
    // GUI components
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JTextField usernameField;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    
    // User data
    private String username = "";
    
    /**
     * Constructor - Sets up the GUI
     */
    public ChatClient() {
        setupGUI();
    }
    
    /**
     * Sets up the graphical user interface
     */
    private void setupGUI() {
        setTitle("Chat Client - Disconnected");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Connection panel at the top
        JPanel connectionPanel = createConnectionPanel();
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        
        // Message display area in the center
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messageArea.setBackground(new Color(248, 248, 248));
        messageArea.setMargin(new Insets(10, 10, 10, 10));
        
        scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel at the bottom
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Status bar
        statusLabel = new JLabel("Not connected to server");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, BorderLayout.PAGE_END);
        
        add(mainPanel);
        
        // Set initial state
        setInputEnabled(false);
        
        // Add window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
        
        // Add welcome message
        appendMessage("=== Welcome to Chat Client ===");
        appendMessage("Enter your username and click Connect to join the chat.");
        appendMessage("=====================================");
    }
    
    /**
     * Creates the connection panel with username input and connect button
     */
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));
        
        panel.add(new JLabel("Username:"));
        
        usernameField = new JTextField(15);
        usernameField.addActionListener(e -> connectToServer());
        panel.add(usernameField);
        
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        return panel;
    }
    
    /**
     * Creates the input panel with text field and send button
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(inputField, BorderLayout.CENTER);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setPreferredSize(new Dimension(80, 25));
        panel.add(sendButton, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Connects to the chat server
     */
    private void connectToServer() {
        if (isConnected) {
            disconnect();
            return;
        }
        
        // Validate username
        String inputUsername = usernameField.getText().trim();
        if (inputUsername.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a username before connecting.", 
                "Username Required", 
                JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }
        
        username = inputUsername;
        
        try {
            // Connect to server
            appendMessage("Connecting to server " + SERVER_HOST + ":" + SERVER_PORT + "...");
            
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            isConnected = true;
            
            // Update GUI
            updateConnectionState(true);
            
            // Send username to server
            out.println(username);
            
            // Start message receiving thread
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
            
            appendMessage("Connected successfully as '" + username + "'");
            inputField.requestFocus();
            
        } catch (IOException e) {
            appendMessage("Connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server.\nPlease make sure the server is running.", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            updateConnectionState(false);
        }
    }
    
    /**
     * Disconnects from the server
     */
    private void disconnect() {
        if (!isConnected) return;
        
        isConnected = false;
        
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
        
        updateConnectionState(false);
        appendMessage("Disconnected from server.");
    }
    
    /**
     * Updates the GUI based on connection state
     */
    private void updateConnectionState(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            isConnected = connected;
            
            if (connected) {
                setTitle("Chat Client - Connected as " + username);
                connectButton.setText("Disconnect");
                statusLabel.setText("Connected to " + SERVER_HOST + ":" + SERVER_PORT);
                statusLabel.setForeground(new Color(0, 128, 0));
                usernameField.setEnabled(false);
                setInputEnabled(true);
            } else {
                setTitle("Chat Client - Disconnected");
                connectButton.setText("Connect");
                statusLabel.setText("Not connected to server");
                statusLabel.setForeground(Color.RED);
                usernameField.setEnabled(true);
                setInputEnabled(false);
            }
        });
    }
    
    /**
     * Enables or disables input components
     */
    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        if (enabled) {
            inputField.requestFocus();
        }
    }
    
    /**
     * Sends a message to the server
     */
    private void sendMessage() {
        if (!isConnected || out == null) return;
        
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        
        // Send message to server
        out.println(message);
        
        // Clear input field
        inputField.setText("");
        inputField.requestFocus();
        
        // Check if message was sent successfully
        if (out.checkError()) {
            appendMessage("Error: Failed to send message. Connection may be lost.");
            disconnect();
        }
    }
    
    /**
     * Receives messages from the server (runs in separate thread)
     */
    private void receiveMessages() {
        try {
            String message;
            while (isConnected && (message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> appendMessage(finalMessage));
            }
        } catch (IOException e) {
            if (isConnected) {
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Connection to server lost: " + e.getMessage());
                    disconnect();
                });
            }
        }
    }
    
    /**
     * Appends a message to the message area
     */
    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            
            // Auto-scroll to bottom
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
            
            // Ensure the scroll pane shows the latest message
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
    
    /**
     * Main method to start the chat client
     */
    public static void main(String[] args) {
        // Create and show the client GUI
        SwingUtilities.invokeLater(() -> {
            new ChatClient().setVisible(true);
        });
        
        System.out.println("Chat Client started");
        System.out.println("Attempting to connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
    }
}