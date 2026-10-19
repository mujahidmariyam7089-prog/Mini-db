package com.mujah.minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PageFileTest {

    @TempDir
    Path tempDir;

    @Test
    void writesReadsAndAppendsPages() throws IOException {
        Path indexFile = tempDir.resolve("index.db");
        PageFile pageFile = new PageFile(indexFile.toString());

        try {
            assertEquals(-1, pageFile.readRootPageId());
            assertEquals(1, pageFile.pageCount());

            Page firstPage = new Page(1, Page.LEAF_PAGE);
            firstPage.keys.add("name");
            firstPage.values.add(10L);

            int firstPageId = pageFile.appendPage(firstPage);
            assertEquals(1, firstPageId);

            Page secondPage = new Page(2, Page.LEAF_PAGE);
            secondPage.keys.add("city");
            secondPage.values.add(30L);

            int secondPageId = pageFile.appendPage(secondPage);
            assertEquals(2, secondPageId);
            assertEquals(3, pageFile.pageCount());

            Page loadedFirst = pageFile.readPage(1);
            assertEquals("name", loadedFirst.keys.get(0));
            assertEquals(10L, loadedFirst.values.get(0));

            Page loadedSecond = pageFile.readPage(2);
            assertEquals("city", loadedSecond.keys.get(0));
            assertEquals(30L, loadedSecond.values.get(0));

            loadedFirst.values.set(0, 99L);
            pageFile.writePage(loadedFirst);

            Page updatedPage = pageFile.readPage(1);
            assertEquals(99L, updatedPage.values.get(0));
        } finally {
            pageFile.close();
        }
    }

    @Test
    void persistsRootPageIdAfterReopening() throws IOException {
        Path indexFile = tempDir.resolve("persistent-index.db");

        try (PageFile writer = new PageFile(indexFile.toString())) {
            writer.writeRootPageId(5);
        }

        try (PageFile reader = new PageFile(indexFile.toString())) {
            assertEquals(5, reader.readRootPageId());
        }
    }

    @Test
    void persistsPagesAfterReopening() throws IOException {
        Path indexFile = tempDir.resolve("persistent-pages.db");

        Page page = new Page(1, Page.LEAF_PAGE);
        page.keys.add("language");
        page.values.add(50L);

        try (PageFile writer = new PageFile(indexFile.toString())) {
            writer.appendPage(page);
        }

        try (PageFile reader = new PageFile(indexFile.toString())) {
            Page loaded = reader.readPage(1);

            assertEquals(2, reader.pageCount());
            assertEquals("language", loaded.keys.get(0));
            assertEquals(50L, loaded.values.get(0));
        }
    }
}