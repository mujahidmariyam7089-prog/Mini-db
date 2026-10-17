package com.mujah.minidb;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PageFile implements AutoCloseable {

    private final RandomAccessFile file;

    public PageFile(String path) throws IOException {
        this.file = new RandomAccessFile(path, "rw");
    }

    public int pageCount() throws IOException {
        return (int) (file.length() / Page.PAGE_SIZE);
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