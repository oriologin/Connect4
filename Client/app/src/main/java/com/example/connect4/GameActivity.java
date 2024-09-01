package com.example.connect4;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    private Connect4Client client; // The client for connecting to the server
    private GridLayout boardGrid; // The grid layout that represents the game board
    private int[][] board = new int[6][7]; // The game board (6 rows x 7 columns)
    private TextView gameStatus; // Text view to display the status of the game
    private TextView startGame; // Text view to show start or end game status
    private EditText enterNameEditText; // EditText to input the player's name
    private Button playAgainButton; // Button to start a new game
    private Button connectButton; // Button to connect to the server
    private boolean gameOver = false; // Flag to indicate if the game is over

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize UI elements
        gameStatus = findViewById(R.id.gameStatus);
        boardGrid = findViewById(R.id.boardGrid);
        enterNameEditText = findViewById(R.id.enterNameEditText);
        playAgainButton = findViewById(R.id.playAgainButton);
        startGame = findViewById(R.id.startGame);
        connectButton = findViewById(R.id.connectButton);

        // Initialize the game board
        initializeBoard();

        // Set up the play again button click listener
        playAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gameOver) {
                    startGame.setText("Starting game...");
                    startGame.setVisibility(View.VISIBLE); // Show the start game TextView
                    resetBoard(); // Reset the board for a new game
                } else {
                    Toast.makeText(GameActivity.this, "The game is not over yet.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up the connect button click listener
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = enterNameEditText.getText().toString();
                if (!playerName.isEmpty()) {
                    // Create a new Connect4Client instance with the player's name
                    client = new Connect4Client(GameActivity.this, playerName);
                    startGame.setText("Connecting...");
                    startGame.setVisibility(View.VISIBLE); // Show the start game TextView
                    client.execute(); // Start the client to connect to the server
                } else {
                    Toast.makeText(GameActivity.this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Handle messages received from the server
    public void handleServerMessage(String message) {
        // Check if the message starts with "state:"
        if (!message.startsWith("state:")) {
            return;
        }

        // Extract and parse the board state from the message
        String boardString = message.substring(6);
        parseBoard(boardString);
        runOnUiThread(this::updateBoardUI); // Update UI on the main thread
    }

    // Method called when connection is successful
    public void onConnectionSuccess() {
        runOnUiThread(() -> {
            startGame.setText("Starting game...");
            Toast.makeText(GameActivity.this, "Connected to the server.", Toast.LENGTH_SHORT).show();
        });
    }

    // Method called when connection fails
    public void onConnectionFailure(String errorMessage) {
        runOnUiThread(() -> {
            startGame.setText("Connection failed");
            Toast.makeText(GameActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    // Parse the board state from a string
    private void parseBoard(String boardString) {
        String[] rows = boardString.split(";");
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < cells.length; j++) {
                board[i][j] = Integer.parseInt(cells[j]);
            }
        }
    }

    // Update the board UI based on the current board state
    private void updateBoardUI() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Button button = (Button) boardGrid.getChildAt(i * 7 + j);
                switch (board[i][j]) {
                    case 1:
                        // Set color for player 1's moves
                        button.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                        break;
                    case 2:
                        // Set color for player 2's moves
                        button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                        break;
                    default:
                        // Set color for empty cells
                        button.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        break;
                }
            }
        }

        // Check the game state (win, tie, or continue)
        int winner = checkWinCondition();
        if (winner > 0) {
            // Message when a player wins
            gameStatus.setText(winner == 1 ? "Congratulations!" : "Game Over");
            gameOver = true;
            startGame.setVisibility(View.INVISIBLE); // Hide the start game TextView
            disableBoard(); // Disable the board to prevent further moves
        } else if (checkTieCondition()) {
            // Message when there is a tie
            gameStatus.setText("Try again!");
            gameOver = true;
            startGame.setVisibility(View.INVISIBLE); // Hide the start game TextView
            disableBoard(); // Disable the board to prevent further moves
        } else {
            gameOver = false;
        }
    }

    // Check if there is a win condition
    private int checkWinCondition() {
        // Check for a win in rows
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col <= board[row].length - 4; col++) {
                int winner = checkLine(board[row][col], board[row][col + 1], board[row][col + 2], board[row][col + 3]);
                if (winner > 0) {
                    return winner;
                }
            }
        }

        // Check for a win in columns
        for (int col = 0; col < board[0].length; col++) {
            for (int row = 0; row <= board.length - 4; row++) {
                int winner = checkLine(board[row][col], board[row + 1][col], board[row + 2][col], board[row + 3][col]);
                if (winner > 0) {
                    return winner;
                }
            }
        }

        // Check for a win in ascending diagonals
        for (int row = 3; row < board.length; row++) {
            for (int col = 0; col <= board[row].length - 4; col++) {
                int winner = checkLine(board[row][col], board[row - 1][col + 1], board[row - 2][col + 2], board[row - 3][col + 3]);
                if (winner > 0) {
                    return winner;
                }
            }
        }

        // Check for a win in descending diagonals
        for (int row = 0; row <= board.length - 4; row++) {
            for (int col = 0; col <= board[row].length - 4; col++) {
                int winner = checkLine(board[row][col], board[row + 1][col + 1], board[row + 2][col + 2], board[row + 3][col + 3]);
                if (winner > 0) {
                    return winner;
                }
            }
        }

        return 0; // No winner
    }

    // Check if a line of four cells has the same value
    private int checkLine(int a, int b, int c, int d) {
        return (a != 0 && a == b && a == c && a == d) ? a : 0;
    }

    // Check if the game is in a tie condition
    private boolean checkTieCondition() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 0) {
                    return false; // There are still empty cells, no tie
                }
            }
        }
        return true; // All cells are filled, it's a tie
    }

    // Reset the game board to start a new game
    private void resetBoard() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                Button cell = (Button) boardGrid.getChildAt(i * 7 + j);
                cell.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                cell.setEnabled(true);
            }
        }
        gameStatus.setText("");
        gameOver = false;
        startGame.setVisibility(View.VISIBLE); // Show the start game TextView

        // Check if the client instance is not null before sending a message
        if (client != null) {
            client.sendMessage("reset");
        } else {
            Toast.makeText(this, "Error: Could not reset the game", Toast.LENGTH_SHORT).show();
            Log.e("GameActivity", "Error: Could not reset the game - Client is null");
        }
    }

    // Disable all buttons on the board
    private void disableBoard() {
        for (int i = 0; i < boardGrid.getChildCount(); i++) {
            View child = boardGrid.getChildAt(i);
            child.setEnabled(false);
        }
    }

    // Initialize the game board UI
    private void initializeBoard() {
        boardGrid.setColumnCount(7);
        boardGrid.setRowCount(6);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                Button cell = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i), GridLayout.spec(j));
                params.width = 100;
                params.height = 100;
                params.setMargins(5, 5, 5, 5);
                cell.setLayoutParams(params);
                cell.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                cell.setEnabled(false);
                cell.setTag(j); // Set the column index as the tag
                cell.setOnClickListener(this::onColumnClick); // Set the click listener
                boardGrid.addView(cell); // Add the button to the grid layout
            }
        }
    }

    // Handle column button clicks
    public void onColumnClick(View view) {
        if (gameOver) {
            return; // Do nothing if the game is over
        }

        int column = Integer.parseInt(view.getTag().toString());
        String message = "move:" + column;
        client.sendMessage(message); // Send the move to the server
    }
}
