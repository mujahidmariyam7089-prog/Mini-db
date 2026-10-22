package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DiskBPlusTreeTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsManyKeysAfterReopening() throws IOException {
        Path indexFile = tempDir.resolve("disk-index.db");

        try (DiskBPlusTree tree = new DiskBPlusTree(indexFile.toString())) {
            for (int i = 0; i < 500; i++) {
                tree.put("key" + i, i);
            }
        }

        try (DiskBPlusTree tree = new DiskBPlusTree(indexFile.toString())) {
            for (int i = 0; i < 500; i++) {
                assertEquals((long) i, tree.get("key" + i));
            }

            tree.put("key250", 999);
            assertEquals(999L, tree.get("key250"));
        }

        try (DiskBPlusTree tree = new DiskBPlusTree(indexFile.toString())) {
            assertEquals(999L, tree.get("key250"));
            assertEquals(249L, tree.get("key249"));
        }
    }

    @Test
    void deletesManyKeysAndStaysCorrectAfterReopening() throws IOException {
        Path indexFile = tempDir.resolve("disk-delete-index.db");

        try (DiskBPlusTree tree = new DiskBPlusTree(indexFile.toString())) {
            for (int i = 0; i < 500; i++) {
                tree.put("key" + i, i);
            }

            for (int i = 0; i < 500; i += 2) {
                tree.remove("key" + i);
            }
        }

        try (DiskBPlusTree tree = new DiskBPlusTree(indexFile.toString())) {
            for (int i = 0; i < 500; i++) {
                Long value = tree.get("key" + i);

                if (i % 2 == 0) {
                    assertNull(value);
                } else {
                    assertEquals((long) i, value);
                }
            }
        }
    }
}