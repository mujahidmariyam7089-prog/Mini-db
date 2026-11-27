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
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniDbEndToEndTest {

    @TempDir
    Path tempDir;

    private MiniDbServer startServer(Database database)
            throws InterruptedException {
        MiniDbServer server =
                new MiniDbServer(0, database, "secret123");

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        long deadline = System.currentTimeMillis() + 3000;

        while (server.getPort() == 0
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertTrue(server.getPort() != 0, "Server did not start");
        return server;
    }

    @Test
    void shouldAuthenticateAndExecuteCommands() throws Exception {
        Database database = new Database(
                tempDir.resolve("e2e.db").toString());
        MiniDbServer server = startServer(database);

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
            assertEquals("OK | 25", reader.readLine());

            writer.println("GET age");
            assertEquals("25", reader.readLine());

            writer.println("INFO");
            String info = reader.readLine();
            assertTrue(info.contains("connected_clients=1"));
            assertTrue(info.contains("cmd_set=2"));
        } finally {
            server.stop();
            database.close();
        }
    }

    @Test
    void shouldRejectCommandsBeforeAuth() throws Exception {
        Database database = new Database(
                tempDir.resolve("auth.db").toString());
        MiniDbServer server = startServer(database);

        try (Socket socket = new Socket("localhost", server.getPort());
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                     socket.getOutputStream(), true)) {

            writer.println("GET name");
            assertEquals("ERR AUTH required", reader.readLine());

            writer.println("SHUTDOWN");
            assertEquals("ERR AUTH required", reader.readLine());

            writer.println("AUTH wrong");
            assertEquals("ERR invalid password", reader.readLine());
        } finally {
            server.stop();
            database.close();
        }
    }

    @Test
    void shouldShutdownWhenAuthenticated() throws Exception {
        Database database = new Database(
                tempDir.resolve("shutdown.db").toString());
        MiniDbServer server = startServer(database);

        try (Socket socket = new Socket("localhost", server.getPort());
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                     socket.getOutputStream(), true)) {

            writer.println("AUTH secret123");
            assertEquals("OK", reader.readLine());

            writer.println("SHUTDOWN");
            assertEquals("OK shutting down", reader.readLine());
        } finally {
            server.stop();
            database.close();
        }
    }
}