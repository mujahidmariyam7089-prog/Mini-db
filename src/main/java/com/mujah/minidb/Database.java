package com.mujah.minidb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable{
    private final String fileName;
    private final DiskManager diskManager;
    private final DiskBPlusTree index;
    private final WalManager wal;

    public Database(String fileName) throws IOException {
        this.fileName = fileName;
        this.diskManager = new DiskManager(fileName);
        this.index = new DiskBPlusTree(fileName + ".index");
        this.wal = new WalManager(fileName + ".wal");

        recoverFromWal();
    }

    private void recoverFromWal() throws IOException {
        List<String> records = wal.readAll();

        for (String record : records) {
            if (record.startsWith("SET ")) {
                String body = record.substring(4);
                int equalsPosition = body.indexOf('=');

                if (equalsPosition > 0) {
                    String key = body.substring(0, equalsPosition);
                    String value = body.substring(equalsPosition + 1);
                    applySet(key, value);
                }
            } else if (record.startsWith("DEL ")) {
                applyDelete(record.substring(4));
            }

        }

        wal.clear();
    }

    private void applySet(String key, String value) throws IOException {
        long newOffset = diskManager.appendLine(key + "=" + value);
        index.put(key, newOffset);
    }

    private void applyDelete(String key) throws IOException {
        index.remove(key);
    }

    public void set(String key, String value) throws IOException {
        wal.append("SET " + key + "=" + value);
        applySet(key, value);
        wal.clear();
    }

    public String get(String key) throws IOException {
        Long offset = index.get(key);

        if (offset == null) {
            return null;
        }

        String line = diskManager.readLineAt(offset);

        if (line == null) {
            return null;
        }

        int equalsPosition = line.indexOf('=');

        if (equalsPosition == -1) {
            return null;
        }

        String storedKey = line.substring(0, equalsPosition);

        if (!storedKey.equals(key)) {
            return null;
        }

        return line.substring(equalsPosition + 1);
    }

    public boolean exists(String key) throws IOException {
        return index.get(key) != null;
    }

    public long increment(String key) throws IOException {
        String currentValue = get(key);
        long number;

        if (currentValue == null) {
            number = 0;
        } else {
            number = Long.parseLong(currentValue);
        }

        number++;
        set(key, String.valueOf(number));

        return number;
    }

    public long decrement(String key) throws IOException {
        String currentValue = get(key);
        long number;

        if (currentValue == null) {
            number = 0;
        } else {
            number = Long.parseLong(currentValue);
        }

        number--;
        set(key, String.valueOf(number));

        return number;
    }

    public int count() throws IOException {
        return index.keys().size();
    }

    public List<String> keys(String pattern) throws IOException {
        List<String> result = new ArrayList<>();

        for (String key : index.keys()) {
            if (matches(key, pattern)) {
                result.add(key);
            }
        }

        return result;
    }

    private boolean matches(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.indexOf('*') == -1) {
            return key.equals(pattern);
        }

        String[] parts = pattern.split("\\*", -1);

        if (parts.length == 2) {
            String prefix = parts[0];
            String suffix = parts[1];

            if (prefix.isEmpty() && suffix.isEmpty()) {
                return true;
            }

            if (!prefix.isEmpty() && !key.startsWith(prefix)) {
                return false;
            }

            if (!suffix.isEmpty() && !key.endsWith(suffix)) {
                return false;
            }

            if (!prefix.isEmpty() && !suffix.isEmpty()
                    && key.length() < prefix.length() + suffix.length()) {
                return false;
            }

            return true;
        }

        return key.equals(pattern);
    }

    public void delete(String key) throws IOException {
        wal.append("DEL " + key);
        applyDelete(key);
        wal.clear();
    }

    public List<String> listAll() throws IOException {
        List<String> result = new ArrayList<>();

        for (String key : index.keys()) {
            String value = get(key);

            if (value != null) {
                result.add(key + " = " + value);
            }
        }

        return result;
    }

    public void compact() throws IOException {
        List<String> keys = index.keys();
        List<String> lines = new ArrayList<>();

        for (String key : keys) {
            String value = get(key);

            if (value != null) {
                lines.add(key + "=" + value);
            }
        }

        List<Long> offsets = diskManager.rewriteAll(lines);

        index.clear();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String key = line.substring(0, line.indexOf('='));
            index.put(key, offsets.get(i));
        }
    }

    public Transaction begin() {
        return new Transaction(this);
    }

    public void close() throws IOException {
        index.close();
        diskManager.close();
        wal.close();
    }

    public String stats() throws IOException {
        long dataSize = diskManager.fileSize();

        java.nio.file.Path indexPath = java.nio.file.Path.of(fileName + ".index");
        long indexSize = java.nio.file.Files.exists(indexPath)
                ? java.nio.file.Files.size(indexPath)
                : 0;

        return "keys: " + count()
                + "\ndata size: " + dataSize + " bytes"
                + "\nindex size: " + indexSize + " bytes";
    }
}