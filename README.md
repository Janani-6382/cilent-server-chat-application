# Java Client-Server Chat Application

A real-time chat application built with Java sockets and multithreading that supports multiple clients communicating simultaneously through a central server.

## Features

- **Multi-client support**: Multiple users can connect and chat simultaneously
- **Real-time messaging**: Instant message delivery between all connected clients
- **Multithreading**: Each client connection handled by a separate server thread
- **User management**: Join/leave notifications and user list tracking
- **Clean disconnection**: Proper handling of client disconnections
- **Cross-platform**: Works on Windows, macOS, and Linux

## Architecture

- **ChatServer**: Central server that manages client connections and message broadcasting
- **ChatClient**: Client application with GUI for users to send and receive messages
- **ClientHandler**: Server-side thread that handles individual client communications

## Quick Start

### 1. Compile the Application

```bash
# Compile all Java files
javac *.java

# Or compile individually
javac ChatServer.java
javac ChatClient.java
javac ClientHandler.java
```

### 2. Start the Server

```bash
java ChatServer
```

The server will start on port 12345 and display:
```
Chat Server started on port 12345
Waiting for clients to connect...
```

### 3. Start Multiple Clients

Open new terminal windows and run:

```bash
# Client 1
java ChatClient

# Client 2 (in another terminal)
java ChatClient

# Client 3 (in another terminal)
java ChatClient
```

Each client will open a GUI window where users can:
- Enter their username
- Send messages to all connected users
- See messages from other users in real-time
- View join/leave notifications

## Usage Example

1. **Start Server**: Run `java ChatServer`
2. **Connect Client 1**: Run `java ChatClient`, enter username "Alice"
3. **Connect Client 2**: Run `java ChatClient`, enter username "Bob"
4. **Chat**: Alice and Bob can now send messages to each other
5. **More Users**: Additional clients can join the conversation

## Project Structure

```
chat-application/
├── ChatServer.java          # Main server class
├── ClientHandler.java       # Handles individual client connections
├── ChatClient.java          # Client GUI application
├── README.md               # This documentation
├── LICENSE                 # MIT License
└── .gitignore             # Git ignore rules
```

## Technical Details

### Server (ChatServer.java)
- Listens on port 12345 for incoming connections
- Creates a new `ClientHandler` thread for each client
- Maintains a list of all connected clients
- Broadcasts messages to all connected users

### Client Handler (ClientHandler.java)
- Runs in a separate thread for each client
- Handles incoming messages from individual clients
- Manages client disconnections gracefully
- Sends join/leave notifications

### Client (ChatClient.java)
- Swing-based GUI with text area for messages and input field
- Separate thread for receiving messages from server
- Clean shutdown handling when window is closed
- Username assignment for message identification

## Message Protocol

- **Join**: `[USERNAME] joined the chat`
- **Leave**: `[USERNAME] left the chat`
- **Chat**: `[USERNAME]: message content`
- **System**: Server status messages

## Error Handling

- Network connection failures
- Client disconnection detection
- Server shutdown gracefully closes all connections
- Invalid input handling
- Socket timeout management

## Customization

### Change Server Port
Modify the `PORT` constant in `ChatServer.java`:
```java
private static final int PORT = 12345; // Change to your preferred port
```

### Modify GUI Appearance
Edit the Swing components in `ChatClient.java`:
- Window size and layout
- Colors and fonts
- Button styles

### Add Features
Potential enhancements:
- Private messaging between users
- Message history persistence
- User authentication
- Emoji support
- File sharing

## Requirements

- Java 8 or higher
- Network connectivity between server and clients
- Multiple terminal windows for testing multiple clients

## Troubleshooting

### Common Issues

1. **"Address already in use"**
   - Wait a few seconds and restart the server
   - Or change the port number

2. **Clients can't connect**
   - Ensure server is running first
   - Check firewall settings
   - Verify correct IP address if running on different machines

3. **Messages not appearing**
   - Check network connectivity
   - Restart both server and clients

### Testing Locally

All components can run on the same machine using `localhost` (127.0.0.1). For testing across different machines, update the server IP address in `ChatClient.java`.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with multiple clients
5. Submit a pull request

