package com.mujah.minidb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class WalManager implements AutoCloseable{

    private final RandomAccessFile file;

    public WalManager(String fileName) throws FileNotFoundException {
        this.file = new RandomAccessFile(fileName, "rw");
    }

    public void append(String record) throws IOException {
        file.seek(file.length());
        file.writeBytes(record + "\n");
    }

    public List<String> readAll() throws IOException {
        List<String> records = new ArrayList<>();
        file.seek(0);

        String line;

        while ((line = file.readLine()) != null) {
            if (!line.isEmpty()) {
                records.add(line);
            }
        }

        return records;
    }

    public void clear() throws IOException {
        file.setLength(0);
        file.seek(0);
    }

    public void close() throws IOException {
        file.close();
    }
}