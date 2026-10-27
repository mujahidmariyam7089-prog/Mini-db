package com.mujah.minidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        Database db = null;

        try {
            db = new Database("data.db");
            CommandParser parser = new CommandParser(db);

            System.out.println("MiniDB started. Type HELP for commands.");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in)
            );

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
                try {
                    db.close();
                } catch (IOException e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }
}