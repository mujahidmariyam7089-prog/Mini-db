package com.mujah.minidb;

import java.util.ArrayList;
import java.util.List;

public class BPlusTree {

    private static final int ORDER = 4;
    private static final int MAX_KEYS = ORDER - 1;

    private BPlusTreeNode root;
    private int size;

    public BPlusTree() {
        this.root = new BPlusTreeNode(true);
        this.size = 0;
    }

    public Long get(String key) {
        BPlusTreeNode leaf = findLeaf(key);
        int index = findKeyIndex(leaf.keys, key);

        if (index < leaf.keys.size()
                && leaf.keys.get(index).equals(key)) {
            return leaf.values.get(index);
        }

        return null;
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    public int size() {
        return size;
    }

    public void put(String key, Long value) {
        Split split = insert(root, key, value);

        if (split != null) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false);
            newRoot.keys.add(split.key);
            newRoot.children.add(root);
            newRoot.children.add(split.right);
            root = newRoot;
        }
    }

    private Split insert(BPlusTreeNode node, String key, Long value) {
        if (node.leaf) {
            int index = findKeyIndex(node.keys, key);

            if (index < node.keys.size()
                    && node.keys.get(index).equals(key)) {
                node.values.set(index, value);
                return null;
            }

            node.keys.add(index, key);
            node.values.add(index, value);
            size++;
        } else {
            int index = findKeyIndex(node.keys, key);
            Split split = insert(node.children.get(index), key, value);

            if (split == null) {
                return null;
            }

            node.keys.add(index, split.key);
            node.children.add(index + 1, split.right);
        }

        if (node.keys.size() <= MAX_KEYS) {
            return null;
        }

        return splitNode(node);
    }

    private Split splitNode(BPlusTreeNode node) {
        int middle = node.keys.size() / 2;
        BPlusTreeNode right = new BPlusTreeNode(node.leaf);
        Split split = new Split();

        if (node.leaf) {
            right.keys.addAll(
                    node.keys.subList(middle, node.keys.size()));
            right.values.addAll(
                    node.values.subList(middle, node.values.size()));

            node.keys.subList(middle, node.keys.size()).clear();
            node.values.subList(middle, node.values.size()).clear();

            right.next = node.next;
            node.next = right;

            split.key = right.keys.get(0);
        } else {
            split.key = node.keys.get(middle);

            right.keys.addAll(
                    node.keys.subList(middle + 1, node.keys.size()));
            right.children.addAll(
                    node.children.subList(middle + 1, node.children.size()));

            node.keys.subList(middle, node.keys.size()).clear();
            node.children.subList(middle + 1, node.children.size()).clear();
        }

        split.right = right;
        return split;
    }

    private BPlusTreeNode findLeaf(String key) {
        BPlusTreeNode node = root;

        while (!node.leaf) {
            int index = findKeyIndex(node.keys, key);
            node = node.children.get(index);
        }

        return node;
    }

    private int findKeyIndex(List<String> keys, String key) {
        int index = 0;

        while (index < keys.size()
                && keys.get(index).compareTo(key) < 0) {
            index++;
        }

        return index;
    }

    private static class Split {
        String key;
        BPlusTreeNode right;
    }

    public List<String> keys() {
        List<String> allKeys = new ArrayList<>();

        BPlusTreeNode node = root;

        while (!node.leaf) {
            node = node.children.get(0);
        }

        while (node != null) {
            allKeys.addAll(node.keys);
            node = node.next;
        }

        return allKeys;
    }

    public void remove(String key) {
        BPlusTreeNode leaf = findLeaf(key);
        int index = findKeyIndex(leaf.keys, key);

        if (index < leaf.keys.size()
                && leaf.keys.get(index).equals(key)) {
            leaf.keys.remove(index);
            leaf.values.remove(index);
            size--;
        }
    }
}