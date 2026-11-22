package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAppendAndReadWalRecords() throws IOException {
        Path walFile = tempDir.resolve("test.wal");

        try (WalManager wal = new WalManager(walFile.toString())) {
            wal.append("SET name=Alice");
            wal.append("SET age=25");

            List<String> records = wal.readAll();

            assertEquals(2, records.size());
            assertEquals("SET name=Alice", records.get(0));
            assertEquals("SET age=25", records.get(1));
        }
    }

    @Test
    void shouldClearWalFile() throws IOException {
        Path walFile = tempDir.resolve("test.wal");

        try (WalManager wal = new WalManager(walFile.toString())) {
            wal.append("SET name=Alice");
            wal.append("DEL name");

            wal.clear();

            assertTrue(wal.readAll().isEmpty());
        }
    }

    @Test
    void shouldRecoverRecordsAfterCloseAndReopen() throws IOException {
        Path walFile = tempDir.resolve("test.wal");

        try (WalManager wal = new WalManager(walFile.toString())) {
            wal.append("SET name=Alice");
            wal.append("SET age=25");
        }

        try (WalManager wal = new WalManager(walFile.toString())) {
            List<String> records = wal.readAll();

            assertEquals(2, records.size());
            assertEquals("SET name=Alice", records.get(0));
            assertEquals("SET age=25", records.get(1));
        }
    }

    @Test
    void shouldAppendAfterClear() throws IOException {
        Path walFile = tempDir.resolve("test.wal");

        try (WalManager wal = new WalManager(walFile.toString())) {
            wal.append("SET old=1");
            wal.clear();
            wal.append("SET new=2");

            List<String> records = wal.readAll();

            assertEquals(1, records.size());
            assertEquals("SET new=2", records.get(0));
        }
    }
}