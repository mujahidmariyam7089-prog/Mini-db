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

            writer.println("STATS");
            String stats = reader.readLine();
            System.out.println(stats);

            writer.println("PING");
            assertEquals("PONG", reader.readLine());
        } finally {
            server.stop();
            database.close();
        }
    }
    private MiniDbServer startServer(Database database, int maxClients)
        throws InterruptedException {
    MiniDbServer server =
            new MiniDbServer(0, database, "secret123", maxClients);

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

    return server;
}

@Test
void shouldRejectClientsOverLimit() throws Exception {
    Database database = new Database(
            tempDir.resolve("limit.db").toString());
    MiniDbServer server = startServer(database, 1);

    try (Socket first = new Socket("localhost", server.getPort());
         PrintWriter firstWriter = new PrintWriter(
                 first.getOutputStream(), true);
         BufferedReader firstReader = new BufferedReader(
                 new InputStreamReader(first.getInputStream()))) {

        firstWriter.println("PING");
        assertEquals("PONG", firstReader.readLine());

        try (Socket second = new Socket("localhost", server.getPort());
             BufferedReader secondReader = new BufferedReader(
                     new InputStreamReader(second.getInputStream()))) {

            assertEquals("ERR max clients reached",
                    secondReader.readLine());
        }
    } finally {
        server.stop();
        database.close();
    }
}
}