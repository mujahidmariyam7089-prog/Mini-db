package com.mujah.minidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class MiniDbServer {

    private final int port;
    private final Database database;
    private ServerSocket serverSocket;
    public MiniDbServer(int port, Database database) {
        this.port = port;
        this.database = database;
    }

    public void start() throws IOException {
    serverSocket = new ServerSocket(port);

    System.out.println(
            "MiniDB listening on port " + serverSocket.getLocalPort());

    while (true) {
        Socket client = serverSocket.accept();
        new Thread(() -> handleClient(client)).start();
    }
}

public int getPort() {
    return serverSocket == null ? port : serverSocket.getLocalPort();
}
    private void handleClient(Socket client) {
        try (client;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream()));
             PrintWriter writer = new PrintWriter(
                     client.getOutputStream(), true)) {

            String line;

            while ((line = reader.readLine()) != null) {
                String response = execute(line);
                writer.println(response);
            }
        } catch (IOException e) {
            System.err.println(
                    "Client error: " + e.getMessage());
        }
    }

    private String execute(String command) {
        try {
            String[] parts = command.trim().split("\\s+", 3);

            if (parts.length == 0 || parts[0].isEmpty()) {
                return "ERROR empty command";
            }

            return switch (parts[0].toUpperCase()) {
                case "SET" -> {
                    if (parts.length < 3) {
                        yield "ERROR usage: SET key value";
                    }
                    database.set(parts[1], parts[2]);
                    yield "OK";
                }
                case "GET" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: GET key";
                    }
                    String value = database.get(parts[1]);
                    yield value == null ? "NULL" : value;
                }
                case "DELETE" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: DELETE key";
                    }
                    database.delete(parts[1]);
                    yield "OK";
                }
                case "EXISTS" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: EXISTS key";
                    }
                    yield database.exists(parts[1])
                            ? "TRUE" : "FALSE";
                }
                case "COUNT" -> String.valueOf(database.count());
                case "INCR" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: INCR key";
                    }
                    yield String.valueOf(database.increment(parts[1]));
                }
                case "DECR" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: DECR key";
                    }
                    yield String.valueOf(database.decrement(parts[1]));
                }
                case "EXPIRE" -> {
                    if (parts.length < 3) {
                        yield "ERROR usage: EXPIRE key seconds";
                    }
                    database.expire(
                            parts[1], Long.parseLong(parts[2]));
                    yield "OK";
                }
                case "TTL" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: TTL key";
                    }
                    Long ttl = database.ttl(parts[1]);
                    yield ttl == null ? "NULL" : String.valueOf(ttl);
                }
                case "KEYS" -> String.join(
                        ",", database.keys(
                                parts.length > 1 ? parts[1] : "*"));
                case "PING" -> "PONG";
                default -> "ERROR unknown command: " + parts[0];
            };
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }
}