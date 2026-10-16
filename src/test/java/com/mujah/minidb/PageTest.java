package com.mujah.minidb;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PageTest {

    @Test
    void serializesAndDeserializesLeafPage() throws IOException {
        Page page = new Page(7, Page.LEAF_PAGE);
        page.nextLeafPageId = 8;

        page.keys.add("name");
        page.keys.add("city");

        page.values.add(0L);
        page.values.add(25L);

        byte[] bytes = page.serialize();
        assertEquals(Page.PAGE_SIZE, bytes.length);

        Page restored = Page.deserialize(bytes);

        assertEquals(7, restored.pageId);
        assertEquals(Page.LEAF_PAGE, restored.pageType);
        assertEquals(8, restored.nextLeafPageId);
        assertEquals(2, restored.keys.size());
        assertEquals("name", restored.keys.get(0));
        assertEquals("city", restored.keys.get(1));
        assertEquals(0L, restored.values.get(0));
        assertEquals(25L, restored.values.get(1));
    }
}