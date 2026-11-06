package com.mujah.minidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MiniDbClient {

    private final String host;
    private final int port;

    public MiniDbClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader serverReader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter serverWriter = new PrintWriter(
                     socket.getOutputStream(), true);
             BufferedReader keyboard = new BufferedReader(
                     new InputStreamReader(System.in))) {

            System.out.println(
                    "Connected to MiniDB at " + host + ":" + port);

            serverWriter.println("AUTH secret123");

            String authResponse = serverReader.readLine();

            if (authResponse == null) {
                System.out.println("Server closed the connection.");
                return;
            }

            System.out.println(authResponse);

            if (!authResponse.equals("OK")) {
                System.out.println("Authentication failed.");
                return;
            }

            System.out.println("Type EXIT to quit.");

            while (true) {
                System.out.print("minidb-client> ");
                String input = keyboard.readLine();

                if (input == null) {
                    break;
                }

                if (input.trim().equalsIgnoreCase("EXIT")
                        || input.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }

                serverWriter.println(input);

                String response = serverReader.readLine();

                if (response == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }

                System.out.println(response);
            }
        }
    }
}