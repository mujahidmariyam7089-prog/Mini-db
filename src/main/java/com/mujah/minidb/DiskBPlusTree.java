package com.mujah.minidb;

import java.io.IOException;

public class DiskBPlusTree implements AutoCloseable {

    private static final int MAX_KEYS = 3;
    private static final int MIN_KEYS = 1;

    private final PageFile file;
    private int rootPageId;

    public DiskBPlusTree(String path) throws IOException {
        this.file = new PageFile(path);
        this.rootPageId = file.readRootPageId();
    }

    public Long get(String key) throws IOException {
        if (rootPageId == -1) {
            return null;
        }

        Page node = file.readPage(rootPageId);

        while (node.pageType == Page.INTERNAL_PAGE) {
            int index = findChildIndex(node.keys, key);
            node = file.readPage(node.childPageIds.get(index));
        }

        int index = findKeyIndex(node.keys, key);

        if (index < node.keys.size()
                && node.keys.get(index).equals(key)) {
            return node.values.get(index);
        }

        return null;
    }

    public void put(String key, long value) throws IOException {
        if (rootPageId == -1) {
            Page leaf = new Page(0, Page.LEAF_PAGE);
            leaf.keys.add(key);
            leaf.values.add(value);

            int leafPageId = file.appendPage(leaf);
            rootPageId = leafPageId;
            file.writeRootPageId(rootPageId);

            return;
        }

        Page root = file.readPage(rootPageId);
        Split split = insert(root, key, value);

        if (split != null) {
            Page newRoot = new Page(0, Page.INTERNAL_PAGE);
            newRoot.keys.add(split.key);
            newRoot.childPageIds.add(rootPageId);
            newRoot.childPageIds.add(split.rightPageId);

            int newRootPageId = file.appendPage(newRoot);

            rootPageId = newRootPageId;
            file.writeRootPageId(rootPageId);
        }
    }

    private Split insert(Page node, String key, long value)
            throws IOException {

        if (node.pageType == Page.LEAF_PAGE) {
            return insertIntoLeaf(node, key, value);
        }

        return insertIntoInternal(node, key, value);
    }

    private Split insertIntoLeaf(Page node, String key, long value)
            throws IOException {

        int index = findKeyIndex(node.keys, key);

        if (index < node.keys.size()
                && node.keys.get(index).equals(key)) {
            node.values.set(index, value);
            file.writePage(node);

            return null;
        }

        node.keys.add(index, key);
        node.values.add(index, value);

        if (node.keys.size() <= MAX_KEYS) {
            file.writePage(node);

            return null;
        }

        int middle = node.keys.size() / 2;

        Page right = new Page(0, Page.LEAF_PAGE);
        right.keys.addAll(
                node.keys.subList(middle, node.keys.size()));
        right.values.addAll(
                node.values.subList(middle, node.values.size()));

        node.keys.subList(middle, node.keys.size()).clear();
        node.values.subList(middle, node.values.size()).clear();

        right.nextLeafPageId = node.nextLeafPageId;

        int rightPageId = file.appendPage(right);
        node.nextLeafPageId = rightPageId;

        file.writePage(node);

        Split split = new Split();
        split.key = right.keys.get(0);
        split.rightPageId = rightPageId;

        return split;
    }

    private Split insertIntoInternal(Page node, String key, long value)
            throws IOException {

        int index = findChildIndex(node.keys, key);
        Page child = file.readPage(node.childPageIds.get(index));

        Split split = insert(child, key, value);

        if (split == null) {
            return null;
        }

        node.keys.add(index, split.key);
        node.childPageIds.add(index + 1, split.rightPageId);

        if (node.keys.size() <= MAX_KEYS) {
            file.writePage(node);

            return null;
        }

        int middle = node.keys.size() / 2;
        String promotedKey = node.keys.get(middle);

        Page right = new Page(0, Page.INTERNAL_PAGE);
        right.keys.addAll(
                node.keys.subList(middle + 1, node.keys.size()));
        right.childPageIds.addAll(
                node.childPageIds.subList(middle + 1, node.childPageIds.size()));

        node.keys.subList(middle, node.keys.size()).clear();
        node.childPageIds.subList(middle + 1, node.childPageIds.size()).clear();

        int rightPageId = file.appendPage(right);
        file.writePage(node);

        Split result = new Split();
        result.key = promotedKey;
        result.rightPageId = rightPageId;

        return result;
    }

    private int findKeyIndex(java.util.List<String> keys, String key) {
        int index = 0;

        while (index < keys.size()
                && keys.get(index).compareTo(key) < 0) {
            index++;
        }

        return index;
    }

    private int findChildIndex(java.util.List<String> keys, String key) {
        int index = 0;

        while (index < keys.size()
                && keys.get(index).compareTo(key) <= 0) {
            index++;
        }

        return index;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private static class Split {
        String key;
        int rightPageId;
    }

    public void remove(String key) throws IOException {
        if (rootPageId == -1) {
            return;
        }

        Page root = file.readPage(rootPageId);
        delete(root, key);

        if (root.pageType == Page.INTERNAL_PAGE
                && root.keys.isEmpty()) {
            rootPageId = root.childPageIds.get(0);
            file.writeRootPageId(rootPageId);
        }
    }

    private boolean delete(Page node, String key) throws IOException {
        if (node.pageType == Page.LEAF_PAGE) {
            int index = findKeyIndex(node.keys, key);

            if (index < node.keys.size()
                    && node.keys.get(index).equals(key)) {
                node.keys.remove(index);
                node.values.remove(index);
                file.writePage(node);

                return node.keys.size() < MIN_KEYS;
            }

            return false;
        }

        int index = findChildIndex(node.keys, key);
        Page child = file.readPage(node.childPageIds.get(index));

        boolean childUnderflow = delete(child, key);

        if (childUnderflow) {
            fixChild(node, index);
        }

        return node.keys.size() < MIN_KEYS;
    }

    private void fixChild(Page parent, int index) throws IOException {
        Page child = file.readPage(parent.childPageIds.get(index));

        Page leftSibling = index > 0
                ? file.readPage(parent.childPageIds.get(index - 1))
                : null;

        Page rightSibling = index < parent.childPageIds.size() - 1
                ? file.readPage(parent.childPageIds.get(index + 1))
                : null;

        if (child.pageType == Page.LEAF_PAGE) {
            fixLeafChild(parent, child, leftSibling, rightSibling, index);
        } else {
            fixInternalChild(parent, child, leftSibling, rightSibling, index);
        }
    }

    private void fixLeafChild(
            Page parent,
            Page child,
            Page leftSibling,
            Page rightSibling,
            int index) throws IOException {

        if (leftSibling != null
                && leftSibling.keys.size() > MIN_KEYS) {
            child.keys.add(0,
                    leftSibling.keys.remove(leftSibling.keys.size() - 1));
            child.values.add(0,
                    leftSibling.values.remove(leftSibling.values.size() - 1));
            parent.keys.set(index - 1, child.keys.get(0));

            file.writePage(child);
            file.writePage(leftSibling);
            file.writePage(parent);

            return;
        }

        if (rightSibling != null
                && rightSibling.keys.size() > MIN_KEYS) {
            child.keys.add(rightSibling.keys.remove(0));
            child.values.add(rightSibling.values.remove(0));
            parent.keys.set(index, rightSibling.keys.get(0));

            file.writePage(child);
            file.writePage(rightSibling);
            file.writePage(parent);

            return;
        }

        if (leftSibling != null) {
            leftSibling.keys.addAll(child.keys);
            leftSibling.values.addAll(child.values);
            leftSibling.nextLeafPageId = child.nextLeafPageId;

            parent.keys.remove(index - 1);
            parent.childPageIds.remove(index);

            file.writePage(leftSibling);
            file.writePage(parent);

            return;
        }

        if (rightSibling != null) {
            child.keys.addAll(rightSibling.keys);
            child.values.addAll(rightSibling.values);
            child.nextLeafPageId = rightSibling.nextLeafPageId;

            parent.keys.remove(index);
            parent.childPageIds.remove(index + 1);

            file.writePage(child);
            file.writePage(parent);
        }
    }

    private void fixInternalChild(
            Page parent,
            Page child,
            Page leftSibling,
            Page rightSibling,
            int index) throws IOException {

        if (leftSibling != null
                && leftSibling.keys.size() > MIN_KEYS) {
            child.keys.add(0, parent.keys.get(index - 1));
            parent.keys.set(index - 1,
                    leftSibling.keys.remove(leftSibling.keys.size() - 1));
            child.childPageIds.add(0,
                    leftSibling.childPageIds.remove(
                            leftSibling.childPageIds.size() - 1));

            file.writePage(child);
            file.writePage(leftSibling);
            file.writePage(parent);

            return;
        }

        if (rightSibling != null
                && rightSibling.keys.size() > MIN_KEYS) {
            child.keys.add(parent.keys.get(index));
            parent.keys.set(index, rightSibling.keys.remove(0));
            child.childPageIds.add(rightSibling.childPageIds.remove(0));

            file.writePage(child);
            file.writePage(rightSibling);
            file.writePage(parent);

            return;
        }

        if (leftSibling != null) {
            leftSibling.keys.add(parent.keys.remove(index - 1));
            leftSibling.keys.addAll(child.keys);
            leftSibling.childPageIds.addAll(child.childPageIds);

            parent.childPageIds.remove(index);

            file.writePage(leftSibling);
            file.writePage(parent);

            return;
        }

        if (rightSibling != null) {
            child.keys.add(parent.keys.remove(index));
            child.keys.addAll(rightSibling.keys);
            child.childPageIds.addAll(rightSibling.childPageIds);

            parent.childPageIds.remove(index + 1);

            file.writePage(child);
            file.writePage(parent);
        }
    }

    public java.util.List<String> keys() throws IOException {
        java.util.List<String> allKeys = new java.util.ArrayList<>();

        if (rootPageId == -1) {
            return allKeys;
        }

        Page node = file.readPage(rootPageId);

        while (node.pageType == Page.INTERNAL_PAGE) {
            node = file.readPage(node.childPageIds.get(0));
        }

        while (node != null) {
            allKeys.addAll(node.keys);

            if (node.nextLeafPageId == -1) {
                break;
            }

            node = file.readPage(node.nextLeafPageId);
        }

        return allKeys;
    }

    public void clear() throws IOException {
        rootPageId = -1;
        file.writeRootPageId(-1);
    }
}