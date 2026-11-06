package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiniDbServerTest {

    @TempDir
    Path tempDir;

    @Test
    void handlesSetAndGetOverSocket() throws Exception {
        Path databaseFile = tempDir.resolve("server.db");
        Database database = new Database(databaseFile.toString());
        MiniDbServer server = new MiniDbServer(0, database, "secret123");

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        int attempts = 0;

        while (server.getPort() == 0 && attempts < 50) {
            Thread.sleep(20);
            attempts++;
        }

        int port = server.getPort();

        try (Socket socket = new Socket("localhost", port);
                PrintWriter writer = new PrintWriter(
                        socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
            writer.println("AUTH secret123");
            assertEquals("OK", reader.readLine());
            writer.println("SET name Mujah");
            assertEquals("OK", reader.readLine());

            writer.println("GET name");
            assertEquals("Mujah", reader.readLine());

            writer.println("PING");
            assertEquals("PONG", reader.readLine());
        }

        database.close();
    }
}