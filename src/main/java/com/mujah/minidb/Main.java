package com.mujah.minidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            startServer();
        } else if (args.length > 0 && args[0].equalsIgnoreCase("client")) {
            startClient(args);
        } else {
            startRepl();
        }
    }

    private static void startServer() {
        Database db = null;

        try {
            db = new Database("data.db");
            MiniDbServer server = new MiniDbServer(6379, db);

            System.out.println("MiniDB server mode. Ctrl+C to stop.");
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());

            if (db != null) {
                closeQuietly(db);
            }
        }
    }

    private static void startRepl() {
        Database db = null;

        try {
            db = new Database("data.db");
            CommandParser parser = new CommandParser(db);

            System.out.println("MiniDB started. Type HELP for commands.");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            while (true) {
                System.out.print("minidb> ");
                String input = reader.readLine();

                if (input == null) {
                    break;
                }

                String result = parser.handle(input);

                if (!result.isEmpty()) {
                    System.out.println(result);
                }

                if (input.trim().equalsIgnoreCase("EXIT")
                        || input.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }

            System.out.println("Goodbye!");
        } catch (IOException e) {
            System.err.println("Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                closeQuietly(db);
            }
        }
    }

    private static void closeQuietly(Database db) {
        try {
            db.close();
        } catch (IOException e) {
            System.err.println(
                    "Error closing database: " + e.getMessage());
        }
    }

    private static void startClient(String[] args) {
        String host = args.length > 1 ? args[1] : "localhost";
        int port = args.length > 2
                ? Integer.parseInt(args[2])
                : 6379;

        try {
            new MiniDbClient(host, port).start();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}