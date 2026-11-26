package com.mujah.minidb.config;

public record ServerConfig(int port, String password, String dataFile) {

    public static ServerConfig fromEnvironment() {
        int port = Integer.parseInt(
                getOrDefault("MINIDB_PORT", "6379"));
        String password = System.getenv("MINIDB_PASSWORD");
        String dataFile = getOrDefault("MINIDB_DATA", "data.db");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "MINIDB_PASSWORD environment variable is required");
        }

        return new ServerConfig(port, password, dataFile);
    }

    private static String getOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}