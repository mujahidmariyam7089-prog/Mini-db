package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExecuteQueuedCommands() throws IOException {
        Database database = new Database(
                tempDir.resolve("transaction.db").toString());

        try {
            database.set("name", "Alice");

            assertEquals("Alice", database.get("name"));
        } finally {
            database.close();
        }
    }

    @Test
    void shouldDeleteKey() throws IOException {
        Database database = new Database(
                tempDir.resolve("transaction.db").toString());

        try {
            database.set("name", "Alice");
            database.delete("name");

            assertNull(database.get("name"));
        } finally {
            database.close();
        }
    }
}