package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void handlesManyKeysCorrectly() throws IOException {
        Path databaseFile = tempDir.resolve("stress-test.db");
        Database database = new Database(databaseFile.toString());

        try {
            for (int i = 0; i < 500; i++) {
                database.set("key" + i, "value" + i);
            }

            assertEquals(500, database.count());

            for (int i = 0; i < 500; i++) {
                assertEquals("value" + i, database.get("key" + i));
            }

            assertTrue(database.keys("key1*").size() >= 100);

            database.delete("key250");
            assertEquals(499, database.count());
            assertFalse(database.exists("key250"));
        } finally {
            database.close();
        }
    }

    @Test
    void compactionRemovesDeletedData() throws IOException {
        Path databaseFile = tempDir.resolve("compact.db");
        Database database = new Database(databaseFile.toString());

        try {
            database.set("name", "Mujah");
            database.set("city", "Mumbai");
            database.set("temp", "delete-me");

            database.delete("temp");
            database.compact();

            assertEquals("Mujah", database.get("name"));
            assertEquals("Mumbai", database.get("city"));
            assertFalse(database.exists("temp"));
            assertEquals(2, database.count());
        } finally {
            database.close();
        }

        Database reopened = new Database(databaseFile.toString());

        try {
            assertEquals("Mujah", reopened.get("name"));
            assertEquals("Mumbai", reopened.get("city"));
            assertFalse(reopened.exists("temp"));
        } finally {
            reopened.close();
        }
    }

    @Test
    void statsShowKeyCountAndFileSizes() throws IOException {
        Path databaseFile = tempDir.resolve("stats.db");
        Database database = new Database(databaseFile.toString());

        try {
            database.set("name", "Mujah");
            database.set("city", "Mumbai");

            String stats = database.stats();

            assertTrue(stats.contains("keys: 2"));
            assertTrue(stats.contains("data size:"));
            assertTrue(stats.contains("index size:"));
        } finally {
            database.close();
        }
    }

    @Test
    void recoversUnfinishedOperationFromWal() throws IOException {
        Path databaseFile = tempDir.resolve("wal-recovery.db");

        Database database = new Database(databaseFile.toString());
        database.set("name", "Mujah");
        database.close();

        try (WalManager wal = new WalManager(databaseFile.toString() + ".wal")) {
            wal.append("SET city=Mumbai");
        }

        Database recovered = new Database(databaseFile.toString());

        try {
            assertEquals("Mumbai", recovered.get("city"));
            assertEquals("Mujah", recovered.get("name"));
        } finally {
            recovered.close();
        }
    }
}