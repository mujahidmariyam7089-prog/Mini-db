package com.mujah.minidb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiskManager {

    private final RandomAccessFile file;

    public DiskManager(String fileName) throws FileNotFoundException {
        this.file = new RandomAccessFile(fileName, "rw");
    }

    public long appendLine(String line) throws IOException {
        long offset = file.length();
        file.seek(offset);

        String data = line + "\n";
        file.writeBytes(data);

        return offset;
    }

    public String readLineAt(long offset) throws IOException {
        file.seek(offset);
        String line = file.readLine();

        if (line == null) {
            return null;
        }

        return new String(
                line.getBytes(StandardCharsets.ISO_8859_1),
                StandardCharsets.UTF_8);
    }

    public List<String> readAllLines() throws IOException {
        List<String> lines = new ArrayList<>();
        file.seek(0);

        String line;

        while ((line = file.readLine()) != null) {
            String utfLine = new String(
                    line.getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);
            lines.add(utfLine);
        }

        return lines;
    }

    public List<Long> rewriteAll(List<String> lines) throws IOException {
        file.setLength(0);
        file.seek(0);

        List<Long> offsets = new ArrayList<>();

        for (String line : lines) {
            offsets.add(file.length());
            file.writeBytes(line + "\n");
        }

        return offsets;
    }

    public void close() throws IOException {
        file.close();
    }
}