package com.example.connect4;

import android.os.AsyncTask;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Connect4Client extends AsyncTask<Void, String, Boolean> {
    private static final String SERVER_IP = "0.0.0.0"; // Replace with your server's IP address
    private static final int SERVER_PORT = 5000;
    private Socket socket;
    private BufferedReader input;
    private PrintStream output;
    private GameActivity gameActivity;
    private String playerName;
    private String connectionErrorMessage;

    public Connect4Client(GameActivity activity, String playerName) {
        this.gameActivity = activity;
        this.playerName = playerName;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintStream(socket.getOutputStream());

            output.println(playerName);
            output.flush();

            // Notify about successful connection
            publishProgress("Connection established successfully!");

            String response;
            while ((response = input.readLine()) != null) {
                publishProgress(response);
            }
            return true;
        } catch (IOException e) {
            connectionErrorMessage = "Error establishing connection to server: " + e.getMessage();
            return false;
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        // Handle server messages
        gameActivity.handleServerMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
            // Show success message if connection was established
            Toast.makeText(gameActivity.getApplicationContext(), "Connection established successfully!", Toast.LENGTH_SHORT).show();
        } else {
            // Show error message if connection failed
            Toast.makeText(gameActivity.getApplicationContext(), connectionErrorMessage, Toast.LENGTH_LONG).show();
        }
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            if (output != null) {
                output.println(message);
                output.flush();
            }
        }).start();
    }
}
