package com.mujah.minidb;

import java.io.IOException;

public class DiskBPlusTree implements AutoCloseable {

    private static final int MAX_KEYS = 3;

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
}