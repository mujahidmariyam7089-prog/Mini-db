package com.mujah.minidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniDbServer {
    private static final Logger log =
            LoggerFactory.getLogger(MiniDbServer.class);

    private static final int DEFAULT_MAX_CLIENTS = 100;
    private static final int IDLE_TIMEOUT_MS = 5 * 60 * 1000;

    private final int port;
    private final Database database;
    private final String password;
    private final int maxClients;
    private final ExecutorService clientPool;
    private volatile ServerSocket serverSocket;
    private volatile boolean running = true;

    private final long startTimeMillis = System.currentTimeMillis();
    private final AtomicInteger clientCount = new AtomicInteger();
    private final AtomicInteger commandCount = new AtomicInteger();
    private final AtomicLong errorCount = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> commandStats =
            new ConcurrentHashMap<>();

    public MiniDbServer(int port, Database database, String password) {
        this(port, database, password, DEFAULT_MAX_CLIENTS);
    }

    public MiniDbServer(int port, Database database,
                        String password, int maxClients) {
        this.port = port;
        this.database = database;
        this.password = password;
        this.maxClients = maxClients;
        this.clientPool = Executors.newFixedThreadPool(maxClients);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        log.info("MiniDB listening on port {}",
                serverSocket.getLocalPort());

        while (running) {
            try {
                Socket client = serverSocket.accept();

                if (clientCount.get() >= maxClients) {
                    rejectClient(client);
                    continue;
                }

                client.setSoTimeout(IDLE_TIMEOUT_MS);
                clientCount.incrementAndGet();
                clientPool.submit(() -> handleClient(client));
            } catch (SocketException e) {
                if (running) {
                    throw e;
                }
            }
        }

        log.info("Server stopped accepting connections");
    }

    private void rejectClient(Socket client) {
        log.warn("Rejecting client {}, server full",
                client.getRemoteSocketAddress());

        try (client;
                PrintWriter out = new PrintWriter(
                        client.getOutputStream(), true)) {
            out.println("ERR max clients reached");
        } catch (IOException e) {
            log.debug("Error rejecting client: {}", e.getMessage());
        }
    }

    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? port : socket.getLocalPort();
    }

    private void handleClient(Socket client) {
        boolean authenticated = false;
        log.info("Client connected: {}", client.getRemoteSocketAddress());

        try (client;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        client.getOutputStream(), true)) {

            boolean inTransaction = false;
            List<String> queuedCommands = new ArrayList<>();

            String line;

            while ((line = reader.readLine()) != null) {
                String command = line.trim();

                if (command.isEmpty()) {
                    continue;
                }

                if (command.equalsIgnoreCase("EXIT")
                        || command.equalsIgnoreCase("QUIT")) {
                    break;
                }

                if (command.equalsIgnoreCase("PING")) {
                    writer.println("PONG");
                    continue;
                }

                if (command.toUpperCase().startsWith("AUTH ")) {
                    String[] parts = command.split("\\s+", 2);

                    if (parts.length == 2
                            && parts[1].equals(password)) {
                        authenticated = true;
                        writer.println("OK");
                    } else {
                        log.warn("Failed AUTH from {}",
                                client.getRemoteSocketAddress());
                        writer.println("ERR invalid password");
                    }

                    continue;
                }

                if (!authenticated) {
                    writer.println("ERR AUTH required");
                    continue;
                }

                if (command.equalsIgnoreCase("SHUTDOWN")) {
                    log.info("SHUTDOWN requested by {}",
                            client.getRemoteSocketAddress());
                    writer.println("OK shutting down");
                    new Thread(this::stop).start();
                    break;
                }

                commandCount.incrementAndGet();

                String firstWord = command.split("\\s+")[0].toUpperCase();
                commandStats
                        .computeIfAbsent(firstWord, k -> new AtomicLong())
                        .incrementAndGet();

                String response;

                if (firstWord.equals("MULTI")) {
                    if (inTransaction) {
                        response = "ERR MULTI cannot be nested";
                    } else {
                        inTransaction = true;
                        queuedCommands.clear();
                        response = "OK";
                    }
                } else if (firstWord.equals("EXEC")) {
                    if (!inTransaction) {
                        response = "ERR EXEC without MULTI";
                    } else {
                        log.debug("Executing {} queued commands",
                                queuedCommands.size());

                        response = executeQueuedCommands(queuedCommands);
                        inTransaction = false;
                        queuedCommands.clear();
                    }
                } else if (firstWord.equals("DISCARD")) {
                    if (!inTransaction) {
                        response = "ERR DISCARD without MULTI";
                    } else {
                        inTransaction = false;
                        queuedCommands.clear();
                        response = "OK";
                    }
                } else if (inTransaction) {
                    if (isValidTransactionCommand(command)) {
                        queuedCommands.add(command);
                        response = "QUEUED";
                    } else {
                        response = "ERR unknown command: " + firstWord;
                    }
                } else {
                    response = execute(command);
                }

                if (response.startsWith("ERR")) {
                    errorCount.incrementAndGet();
                }

                writer.println(response);
            }
        } catch (SocketTimeoutException e) {
            log.info("Closing idle client");
        } catch (IOException e) {
            if (running) {
                log.error("Client error: {}", e.getMessage());
            }
        } finally {
            clientCount.decrementAndGet();
            log.info("Client disconnected");
        }
    }

    private String buildInfo() throws IOException {
        long uptimeSeconds =
                (System.currentTimeMillis() - startTimeMillis) / 1000;

        StringBuilder info = new StringBuilder();
        info.append("uptime_seconds=").append(uptimeSeconds);
        info.append(",connected_clients=").append(clientCount.get());
        info.append(",total_commands=").append(commandCount.get());
        info.append(",errors=").append(errorCount.get());
        info.append(",keys=").append(database.count());

        commandStats.forEach((name, count) ->
                info.append(",cmd_")
                        .append(name.toLowerCase())
                        .append("=")
                        .append(count.get()));

        return info.toString();
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
                    yield database.exists(parts[1]) ? "TRUE" : "FALSE";
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
                    database.expire(parts[1], Long.parseLong(parts[2]));
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
                        ",",
                        database.keys(parts.length > 1 ? parts[1] : "*"));
                case "HELP" -> """
                        Commands:
                        AUTH password
                        SET key value
                        GET key
                        DELETE key
                        EXISTS key
                        COUNT
                        INCR key
                        DECR key
                        EXPIRE key seconds
                        TTL key
                        KEYS pattern
                        GETDEL key
                        FLUSHALL - delete all keys
                        STATS - connected clients, commands processed, key count
                        INFO - uptime, clients, errors, per-command counts
                        TYPE key - show string or none
                        APPEND key value - add value to the end of a value
                        MULTI / EXEC / DISCARD - transactions
                        PING
                        SHUTDOWN
                        EXIT
                        """;
                case "STATS" -> "connected_clients=" + clientCount.get()
                        + ",commands_processed=" + commandCount.get()
                        + ",keys=" + database.count();
                case "INFO" -> buildInfo();
                case "GETDEL" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: GETDEL key";
                    }

                    String value = database.get(parts[1]);

                    if (value == null) {
                        yield "NULL";
                    }

                    database.delete(parts[1]);
                    yield value;
                }
                case "FLUSHALL" -> {
                    for (String key : database.keys("*")) {
                        database.delete(key);
                    }

                    yield "OK";
                }
                case "TYPE" -> {
                    if (parts.length < 2) {
                        yield "ERROR usage: TYPE key";
                    }

                    String value = database.get(parts[1]);
                    yield value == null ? "none" : "string";
                }
                case "APPEND" -> {
                    if (parts.length < 3) {
                        yield "ERROR usage: APPEND key value";
                    }

                    String current = database.get(parts[1]);
                    String updated =
                            (current == null ? "" : current) + parts[2];

                    database.set(parts[1], updated);
                    yield updated;
                }
                default -> "ERROR unknown command: " + parts[0];
            };
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    private String executeQueuedCommands(List<String> queuedCommands) {
        if (queuedCommands.isEmpty()) {
            return "OK";
        }

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < queuedCommands.size(); i++) {
            if (i > 0) {
                output.append(" | ");
            }

            output.append(execute(queuedCommands.get(i)));
        }

        return output.toString();
    }

    public synchronized void stop() {
        running = false;

        ServerSocket socket = serverSocket;

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                log.info("Server socket closed");
            } catch (IOException e) {
                log.warn("Error stopping server: {}", e.getMessage());
            }
        }

        clientPool.shutdown();

        try {
            if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidTransactionCommand(String command) {
        String firstWord = command.split("\\s+")[0].toUpperCase();

        return switch (firstWord) {
            case "SET", "GET", "DELETE", "EXISTS", "COUNT",
                    "INCR", "DECR", "EXPIRE", "TTL", "KEYS",
                    "GETDEL", "TYPE", "APPEND", "STATS", "INFO" ->
                true;
            default -> false;
        };
    }
}