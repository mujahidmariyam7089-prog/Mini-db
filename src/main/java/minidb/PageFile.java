package com.mujah.minidb;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PageFile implements AutoCloseable {

    private static final int HEADER_PAGE_ID = 0;
    private static final int NO_ROOT = -1;

    private final RandomAccessFile file;

    public PageFile(String path) throws IOException {
        this.file = new RandomAccessFile(path, "rw");

        if (file.length() == 0) {
            writeRootPageId(NO_ROOT);
        }
    }

    public int pageCount() throws IOException {
        return (int) (file.length() / Page.PAGE_SIZE);
    }

    public int readRootPageId() throws IOException {
        file.seek(0);
        return file.readInt();
    }

    public void writeRootPageId(int rootPageId) throws IOException {
        byte[] header = new byte[Page.PAGE_SIZE];

        header[0] = (byte) (rootPageId >>> 24);
        header[1] = (byte) (rootPageId >>> 16);
        header[2] = (byte) (rootPageId >>> 8);
        header[3] = (byte) rootPageId;

        file.seek(0);
        file.write(header);
    }

    public Page readPage(int pageId) throws IOException {
        long position = (long) pageId * Page.PAGE_SIZE;

        if (position + Page.PAGE_SIZE > file.length()) {
            throw new IOException(
                    "Page does not exist: " + pageId);
        }

        file.seek(position);

        byte[] bytes = new byte[Page.PAGE_SIZE];
        file.readFully(bytes);

        return Page.deserialize(bytes);
    }

    public void writePage(Page page) throws IOException {
        if (page.pageId == HEADER_PAGE_ID) {
            throw new IOException(
                    "Page 0 is reserved for the database header");
        }

        long position = (long) page.pageId * Page.PAGE_SIZE;

        file.seek(position);
        file.write(page.serialize());
    }

    public int appendPage(Page page) throws IOException {
        int pageId = pageCount();
        page.pageId = pageId;

        writePage(page);

        return pageId;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}