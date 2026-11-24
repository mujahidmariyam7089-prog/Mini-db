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

class MiniDbEndToEndTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAuthenticateAndExecuteCommands() throws IOException {
        Database database = new Database(
                tempDir.resolve("e2e.db").toString());

        MiniDbServer server = new MiniDbServer(0, database, "secret123");

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected when the test shuts the server down.
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(200);

            try (Socket socket = new Socket("localhost", server.getPort());
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(
                         socket.getOutputStream(), true)) {

                writer.println("AUTH secret123");
                assertEquals("OK", reader.readLine());

                writer.println("SET name Alice");
                assertEquals("OK", reader.readLine());

                writer.println("GET name");
                assertEquals("Alice", reader.readLine());

                writer.println("MULTI");
                assertEquals("OK", reader.readLine());

                writer.println("SET age 25");
                assertEquals("QUEUED", reader.readLine());

                writer.println("GET age");
                assertEquals("QUEUED", reader.readLine());

                writer.println("EXEC");
                assertEquals("OK | Alice", reader.readLine());

                writer.println("GET age");
                assertEquals("25", reader.readLine());
            }
        } finally {
            server.stop();
            database.close();
        }
    }
}