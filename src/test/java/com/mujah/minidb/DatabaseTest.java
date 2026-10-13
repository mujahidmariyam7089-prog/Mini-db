package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mujah.minidb.Database;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @TempDir
    Path tempDir;

    @Test
    void setGetAndDeleteWork() throws IOException {
        Path databaseFile = tempDir.resolve("test.db");
        Database database = new Database(databaseFile.toString());

        try {
            database.set("name", "Mujah");

            assertEquals("Mujah", database.get("name"));
            assertTrue(database.exists("name"));

            database.delete("name");

            assertFalse(database.exists("name"));
            assertNull(database.get("name"));
        } finally {
            database.close();
        }
    }

    @Test
    void dataPersistsAfterReopening() throws IOException {
        Path databaseFile = tempDir.resolve("persistence.db");

        Database firstDatabase = new Database(databaseFile.toString());

        try {
            firstDatabase.set("name", "Mujah");
        } finally {
            firstDatabase.close();
        }

        Database secondDatabase = new Database(databaseFile.toString());

        try {
            assertEquals("Mujah", secondDatabase.get("name"));
            assertTrue(secondDatabase.exists("name"));
        } finally {
            secondDatabase.close();
        }
    }

    @Test
    void incrementAndDecrementWork() throws IOException {
        Path databaseFile = tempDir.resolve("counters.db");
        Database database = new Database(databaseFile.toString());

        try {
            assertEquals(1, database.increment("visits"));
            assertEquals(2, database.increment("visits"));
            assertEquals(1, database.decrement("visits"));
            assertEquals("1", database.get("visits"));
        } finally {
            database.close();
        }
    }

    @Test
    void countAndKeyPatternsWork() throws IOException {
        Path databaseFile = tempDir.resolve("keys.db");
        Database database = new Database(databaseFile.toString());

        try {
            database.set("user:1", "Ibrahim");
            database.set("user:2", "Mariyam");
            database.set("post:1", "Mayank");

            assertEquals(3, database.count());
            assertEquals(2, database.keys("user:*").size());
            assertTrue(database.keys("user:*").contains("user:1"));
            assertTrue(database.keys("user:*").contains("user:2"));

            database.delete("user:1");

            assertEquals(2, database.count());
            assertFalse(database.keys("user:*").contains("user:1"));
        } finally {
            database.close();
        }
    }

    @Test
    void handlesMissingKeysAndInvalidCounters() throws IOException {
        Path databaseFile = tempDir.resolve("edge-cases.db");
        Database database = new Database(databaseFile.toString());

        try {
            assertNull(database.get("missing"));
            assertFalse(database.exists("missing"));
            assertEquals(0, database.count());

            database.set("name", "Mujah");

            assertThrows(NumberFormatException.class, () -> {
                database.increment("name");
            });

            assertTrue(database.keys("city:*").isEmpty());

            database.delete("does-not-exist");
            assertEquals(1, database.count());
        } finally {
            database.close();
        }
    }
}