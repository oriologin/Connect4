package Practicas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainServer {
    // Define the port on which the server will listen
    private static final int PORT = 5000;
    // List to keep track of connected clients
    private static final List<ClientHandler> clients = new ArrayList<>();
    // Game instance to manage the game state
    private static final Connect4Game game = new Connect4Game();

    public static void main(String[] args) {
        try {
            // Create a ServerSocket to listen for incoming connections
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server> Server started");
            System.out.println("Server> Waiting for clients...");

            // Loop to accept client connections
            while (true) {
                // Accept an incoming client connection
                Socket clientSocket = serverSocket.accept();
                InetAddress clientAddress = clientSocket.getInetAddress();
                String clientName = clientAddress.getHostName(); // Get the client's hostname
                System.out.println("Server> New client connected: " + clientName + " (" + clientAddress + ")");
                System.out.println("Connection established with client: " + clientName); // Log new connection
                // Create a new ClientHandler to manage the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // Add the handler to the list of clients
                clients.add(clientHandler);
                // Start the ClientHandler in a new thread
                clientHandler.start();
            }
        } catch (IOException ex) {
            // Handle exceptions related to server startup or IO operations
            System.err.println("Error starting server: " + ex.getMessage());
        }
    }

    // Inner class to handle communication with a connected client
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintStream output;
        private String playerName;
        private int playerNumber = 1; // The client is always player 1
        private static final int AI_PLAYER = 2; // The AI is player 2

        // Constructor to initialize the ClientHandler with a socket
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                // Initialize input and output streams
                this.input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.output = new PrintStream(clientSocket.getOutputStream());
            } catch (IOException ex) {
                // Handle exceptions related to stream initialization
                System.err.println("Error getting input/output streams: " + ex.getMessage());
            }
        }

        // Method to handle client communication
        public void run() {
            try {
                // Request the player's name
                sendMessage("Welcome to Connect 4! Enter your name:");
                playerName = input.readLine();
                System.out.println("Server> " + playerName + " has joined as Player " + playerNumber);

                // Send the initial game state to the player
                sendGameState();

                // Loop to continuously read messages from the client
                while (true) {
                    String message = input.readLine();
                    if (message != null) {
                        handleClientMessage(message);
                    }
                }
            } catch (IOException ex) {
                // Handle exceptions related to client communication
                System.err.println("Error handling client connection: " + ex.getMessage());
            } finally {
                try {
                    // Close the client socket and remove the handler from the list
                    clientSocket.close();
                    clients.remove(this);
                    System.out.println("Server> " + playerName + " disconnected");
                } catch (IOException ex) {
                    // Handle exceptions related to closing the socket
                    System.err.println("Error closing client socket: " + ex.getMessage());
                }
            }
        }

        // Method to handle messages received from the client
        private void handleClientMessage(String message) {
            if (message.startsWith("move:")) {
                try {
                    // Extract the column from the message
                    int column = Integer.parseInt(message.split(":")[1]);
                    // Make the move on the game board
                    boolean success = game.makeMove(playerNumber, column);
                    if (success) {
                        // Notify all clients of the updated game state
                        sendGameStateToAll();
                        // AI's turn
                        if (!game.isGameOver()) {
                            int aiMove = game.getRandomMove();
                            game.makeMove(AI_PLAYER, aiMove);
                            sendGameStateToAll();
                        }
                    } else {
                        sendMessage("Invalid move, try again.");
                    }
                } catch (NumberFormatException e) {
                    sendMessage("Invalid move format.");
                }
            } else if (message.equals("reset")) {
                // Reset the game if requested
                game.resetGame();
                sendGameStateToAll();
            }
        }

        // Method to send a message to the client
        private void sendMessage(String message) {
            output.println(message);
            output.flush();
        }

        // Method to send the current game state to the client
        private void sendGameState() {
            output.println("state:" + formatBoard(game.getBoard()));
            output.flush();
        }

        // Method to send the current game state to all connected clients
        private void sendGameStateToAll() {
            String boardState = "state:" + formatBoard(game.getBoard());
            for (ClientHandler client : clients) {
                client.output.println(boardState);
                client.output.flush();
            }
        }

        // Method to format the game board as a string
        private String formatBoard(int[][] board) {
            StringBuilder builder = new StringBuilder();
            for (int[] row : board) {
                for (int cell : row) {
                    builder.append(cell).append(",");
                }
                builder.setLength(builder.length() - 1); // Remove the last comma
                builder.append(";");
            }
            builder.setLength(builder.length() - 1); // Remove the last semicolon
            return builder.toString();
        }
    }

    // Inner class to manage the Connect 4 game logic
    private static class Connect4Game {
        private static final int ROWS = 6;
        private static final int COLUMNS = 7;
        private final int[][] board = new int[ROWS][COLUMNS];
        private int currentPlayer = 1;

        // Get the current state of the board
        public int[][] getBoard() {
            return board;
        }

        // Make a move on the board
        public boolean makeMove(int player, int column) {
            if (column < 0 || column >= COLUMNS) return false;
            for (int row = ROWS - 1; row >= 0; row--) {
                if (board[row][column] == 0) {
                    board[row][column] = player;
                    currentPlayer = 3 - currentPlayer; // Alternate between player 1 and 2
                    return true;
                }
            }
            return false;
        }

        // Check if the game is over (win or tie)
        public boolean isGameOver() {
            // Check for a win in rows
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col <= COLUMNS - 4; col++) {
                    if (board[row][col] != 0 &&
                            board[row][col] == board[row][col + 1] &&
                            board[row][col] == board[row][col + 2] &&
                            board[row][col] == board[row][col + 3]) {
                        return true;
                    }
                }
            }

            // Check for a win in columns
            for (int col = 0; col < COLUMNS; col++) {
                for (int row = 0; row <= ROWS - 4; row++) {
                    if (board[row][col] != 0 &&
                            board[row][col] == board[row + 1][col] &&
                            board[row][col] == board[row + 2][col] &&
                            board[row][col] == board[row + 3][col]) {
                        return true;
                    }
                }
            }

            // Check for a win in ascending diagonals
            for (int row = 3; row < ROWS; row++) {
                for (int col = 0; col <= COLUMNS - 4; col++) {
                    if (board[row][col] != 0 &&
                            board[row][col] == board[row - 1][col + 1] &&
                            board[row][col] == board[row - 2][col + 2] &&
                            board[row][col] == board[row - 3][col + 3]) {
                        return true;
                    }
                }
            }

            // Check for a win in descending diagonals
            for (int row = 0; row <= ROWS - 4; row++) {
                for (int col = 0; col <= COLUMNS - 4; col++) {
                    if (board[row][col] != 0 &&
                            board[row][col] == board[row + 1][col + 1] &&
                            board[row][col] == board[row + 2][col + 2] &&
                            board[row][col] == board[row + 3][col + 3]) {
                        return true;
                    }
                }
            }

            // Check for a tie
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    if (board[row][col] == 0) {
                        return false; // Still empty cells, game is not over
                    }
                }
            }
            return true; // All cells are occupied, it's a tie
        }

        // Get a random move for the AI
        public int getRandomMove() {
            Random random = new Random();
            int column;
            do {
                column = random.nextInt(COLUMNS);
            } while (!isColumnAvailable(column));
            return column;
        }

        // Check if a column is available for a move
        private boolean isColumnAvailable(int column) {
            return board[0][column] == 0;
        }

        // Reset the game board to its initial state
        public void resetGame() {
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLUMNS; j++) {
                    board[i][j] = 0;
                }
            }
            currentPlayer = 1; // Start with player 1
        }
    }
}
