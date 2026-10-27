package com.mujah.minidb;

import java.io.IOException;
import java.util.List;

public class CommandParser {

    private final Database db;

    public CommandParser(Database db) {
        this.db = db;
    }

    public String handle(String input) throws IOException {
        input = input.trim();

        if (input.isEmpty()) {
            return "";
        }

        String[] parts = input.split("\\s+", 3);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "HELP":
                return help();

            case "SET":
                return handleSet(parts);

            case "GET":
                return handleGet(parts);

            case "DELETE":
                return handleDelete(parts);

            case "LIST":
                return handleList();

            case "EXISTS":
                return handleExists(parts);

            case "INCR":
                return handleIncrement(parts);

            case "DECR":
                return handleDecrement(parts);

            case "COUNT":
                return handleCount();

            case "KEYS":
                return handleKeys(parts);

            case "COMPACT":
                return handleCompact();

            case "STATS":
                return handleStats();

            case "EXIT":
            case "QUIT":
                return "Exiting...";

            default:
                return "Unknown command: " + command + ". Type HELP.";
        }
    }

    private String help() {
        return """
                Available commands:

                  SET <key> <value>    Store a value
                  GET <key>            Read a value
                  DELETE <key>         Delete a key
                  LIST                 List all keys
                  EXISTS <key>         Check whether a key exists
                  INCR <key>           Increase a numeric value by 1
                  DECR <key>           Decrease a numeric value by 1
                  COUNT                Show the number of stored keys
                  KEYS <pattern>       List keys matching a pattern
                  COMPACT              Reclaim space from deleted keys
                  STATS                Show database statistics
                  HELP                 Show commands
                  EXIT                 Quit the database
                """;
    }

    private String handleSet(String[] parts) throws IOException {
        if (parts.length < 3) {
            return "Usage: SET <key> <value>";
        }

        db.set(parts[1], parts[2]);
        return "OK";
    }

    private String handleGet(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: GET <key>";
        }

        String value = db.get(parts[1]);
        return value != null ? value : "(nil)";
    }

    private String handleDelete(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: DELETE <key>";
        }

        db.delete(parts[1]);
        return "OK";
    }

    private String handleList() throws IOException {
        List<String> all = db.listAll();

        if (all.isEmpty()) {
            return "(empty)";
        }

        return String.join("\n", all);
    }

    private String handleExists(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: EXISTS <key>";
        }

        boolean exists = db.exists(parts[1]);
        return exists ? "true" : "false";
    }

    private String handleIncrement(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: INCR <key>";
        }

        try {
            long newValue = db.increment(parts[1]);
            return String.valueOf(newValue);
        } catch (NumberFormatException e) {
            return "ERR value is not an integer";
        }
    }

    private String handleDecrement(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: DECR <key>";
        }

        try {
            long newValue = db.decrement(parts[1]);
            return String.valueOf(newValue);
        } catch (NumberFormatException e) {
            return "ERR value is not an integer";
        }
    }

    private String handleCount() throws IOException {
        int total = db.count();
        return String.valueOf(total);
    }

    private String handleKeys(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Usage: KEYS <pattern>";
        }

        List<String> keys = db.keys(parts[1]);

        if (keys.isEmpty()) {
            return "(empty)";
        }

        return String.join("\n", keys);
    }

    private String handleCompact() throws IOException {
        db.compact();
        return "OK";
    }

    private String handleStats() throws IOException {
        return db.stats();
    }
}