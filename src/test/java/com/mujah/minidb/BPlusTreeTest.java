package com.mujah.minidb;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BPlusTreeTest {

    @Test
    void insertsSearchesAndDeletesManyKeys() {
        BPlusTree tree = new BPlusTree();

        List<String> keys = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            keys.add("key" + i);
        }

        Collections.shuffle(keys);

        for (String key : keys) {
            tree.put(key, 100L);
        }

        assertEquals(1000, tree.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals(100L, tree.get("key" + i));
        }

        List<String> storedKeys = tree.keys();
        assertEquals(1000, storedKeys.size());

        for (int i = 0; i < 999; i++) {
            assertTrue(
                    storedKeys.get(i)
                            .compareTo(storedKeys.get(i + 1)) < 0);
        }

        tree.remove("key500");
        assertNull(tree.get("key500"));
        assertEquals(999, tree.size());
        assertEquals(100L, tree.get("key499"));
    }

    @Test
    void updatesExistingKeys() {
        BPlusTree tree = new BPlusTree();

        tree.put("name", 10L);
        tree.put("name", 20L);

        assertEquals(20L, tree.get("name"));
        assertEquals(1, tree.size());
    }

    @Test
    void handlesMissingKeys() {
        BPlusTree tree = new BPlusTree();

        assertNull(tree.get("missing"));
        assertFalse(tree.containsKey("missing"));

        tree.remove("missing");

        assertEquals(0, tree.size());
    }
}