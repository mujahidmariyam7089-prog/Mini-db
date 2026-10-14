package com.mujah.minidb;

import java.util.ArrayList;
import java.util.List;

public class BPlusTree {

    private static final int ORDER = 4;
    private static final int MAX_KEYS = ORDER - 1;
    private static final int MIN_KEYS = 1;

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
            int index = findChildIndex(node.keys, key);
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
            int index = findChildIndex(node.keys, key);
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

    private int findChildIndex(List<String> keys, String key) {
        int index = 0;

        while (index < keys.size()
                && keys.get(index).compareTo(key) <= 0) {
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
        delete(root, key);

        if (!root.leaf && root.keys.isEmpty()) {
            root = root.children.get(0);
        }
    }

    private boolean delete(BPlusTreeNode node, String key) {
        if (node.leaf) {
            int index = findKeyIndex(node.keys, key);

            if (index < node.keys.size()
                    && node.keys.get(index).equals(key)) {
                node.keys.remove(index);
                node.values.remove(index);
                size--;
                return node.keys.size() < MIN_KEYS;
            }

            return false;
        }

        int index = findChildIndex(node.keys, key);
        boolean childUnderflow = delete(node.children.get(index), key);

        if (childUnderflow) {
            fixChild(node, index);
        }

        return node.keys.size() < MIN_KEYS;
    }

    private void fixChild(BPlusTreeNode parent, int index) {
        BPlusTreeNode child = parent.children.get(index);
        BPlusTreeNode leftSibling = index > 0 ? parent.children.get(index - 1) : null;
        BPlusTreeNode rightSibling = index < parent.children.size() - 1
                ? parent.children.get(index + 1)
                : null;

        if (child.leaf) {
            fixLeafChild(parent, child, leftSibling, rightSibling, index);
        } else {
            fixInternalChild(parent, child, leftSibling, rightSibling, index);
        }
    }

    private void fixLeafChild(
            BPlusTreeNode parent,
            BPlusTreeNode child,
            BPlusTreeNode leftSibling,
            BPlusTreeNode rightSibling,
            int index) {

        if (leftSibling != null
                && leftSibling.keys.size() > MIN_KEYS) {
            child.keys.add(0, leftSibling.keys.remove(leftSibling.keys.size() - 1));
            child.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
            parent.keys.set(index - 1, child.keys.get(0));
            return;
        }

        if (rightSibling != null
                && rightSibling.keys.size() > MIN_KEYS) {
            child.keys.add(rightSibling.keys.remove(0));
            child.values.add(rightSibling.values.remove(0));
            parent.keys.set(index, rightSibling.keys.get(0));
            return;
        }

        if (leftSibling != null) {
            leftSibling.keys.addAll(child.keys);
            leftSibling.values.addAll(child.values);
            leftSibling.next = child.next;
            parent.keys.remove(index - 1);
            parent.children.remove(index);
            return;
        }

        if (rightSibling != null) {
            child.keys.addAll(rightSibling.keys);
            child.values.addAll(rightSibling.values);
            child.next = rightSibling.next;
            parent.keys.remove(index);
            parent.children.remove(index + 1);
        }
    }

    private void fixInternalChild(
            BPlusTreeNode parent,
            BPlusTreeNode child,
            BPlusTreeNode leftSibling,
            BPlusTreeNode rightSibling,
            int index) {

        if (leftSibling != null
                && leftSibling.keys.size() > MIN_KEYS) {
            child.keys.add(0, parent.keys.get(index - 1));
            parent.keys.set(index - 1,
                    leftSibling.keys.remove(leftSibling.keys.size() - 1));
            child.children.add(0,
                    leftSibling.children.remove(leftSibling.children.size() - 1));
            return;
        }

        if (rightSibling != null
                && rightSibling.keys.size() > MIN_KEYS) {
            child.keys.add(parent.keys.get(index));
            parent.keys.set(index, rightSibling.keys.remove(0));
            child.children.add(rightSibling.children.remove(0));
            return;
        }

        if (leftSibling != null) {
            leftSibling.keys.add(parent.keys.remove(index - 1));
            leftSibling.keys.addAll(child.keys);
            leftSibling.children.addAll(child.children);
            parent.children.remove(index);
            return;
        }

        if (rightSibling != null) {
            child.keys.add(parent.keys.remove(index));
            child.keys.addAll(rightSibling.keys);
            child.children.addAll(rightSibling.children);
            parent.children.remove(index + 1);
        }
    }
}