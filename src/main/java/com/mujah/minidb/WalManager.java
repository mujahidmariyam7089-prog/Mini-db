package com.mujah.minidb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class WalManager implements AutoCloseable {

    private final RandomAccessFile file;

    public WalManager(String fileName) throws FileNotFoundException {
        this.file = new RandomAccessFile(fileName, "rw");
    }

    public void append(String record) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(record.getBytes());

        String checksum = Long.toString(crc.getValue());

        file.seek(file.length());
        file.writeBytes(checksum + "|" + record + "\n");
        file.getFD().sync();
    }

    public List<String> readAll() throws IOException {
        List<String> records = new ArrayList<>();

        file.seek(0);

        String line;

        while ((line = file.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }

            int separator = line.indexOf('|');

            if (separator == -1) {
                throw new IOException(
                        "Corrupted WAL record: missing checksum");
            }

            String checksum = line.substring(0, separator);
            String record = line.substring(separator + 1);

            CRC32 crc = new CRC32();
            crc.update(record.getBytes());

            if (!checksum.equals(Long.toString(crc.getValue()))) {
                throw new IOException(
                        "Corrupted WAL record: checksum mismatch");
            }

            records.add(record);
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