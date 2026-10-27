package com.mujah.minidb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Page {

    public static final int PAGE_SIZE = 4096;

    public static final int LEAF_PAGE = 1;
    public static final int INTERNAL_PAGE = 2;

    public int pageId;
    public int pageType;
    public int nextLeafPageId;
    public List<String> keys;
    public List<Long> values;
    public List<Integer> childPageIds;

    public Page(int pageId, int pageType) {
        this.pageId = pageId;
        this.pageType = pageType;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.childPageIds = new ArrayList<>();
        this.nextLeafPageId = -1;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);

        out.writeInt(pageId);
        out.writeInt(pageType);
        out.writeInt(nextLeafPageId);
        out.writeInt(keys.size());

        for (String key : keys) {
            out.writeUTF(key);
        }

        if (pageType == LEAF_PAGE) {
            for (Long value : values) {
                out.writeLong(value);
            }
        } else {
            out.writeInt(childPageIds.size());

            for (Integer childPageId : childPageIds) {
                out.writeInt(childPageId);
            }
        }

        byte[] data = byteStream.toByteArray();

        if (data.length > PAGE_SIZE) {
            throw new IOException(
                    "Page is too large: " + data.length + " bytes");
        }

        byte[] page = new byte[PAGE_SIZE];
        System.arraycopy(data, 0, page, 0, data.length);

        return page;
    }

    public static Page deserialize(byte[] page) throws IOException {
        ByteArrayInputStream byteStream =
                new ByteArrayInputStream(page);
        DataInputStream in = new DataInputStream(byteStream);

        int pageId = in.readInt();
        int pageType = in.readInt();
        int nextLeafPageId = in.readInt();
        int keyCount = in.readInt();

        Page result = new Page(pageId, pageType);
        result.nextLeafPageId = nextLeafPageId;

        for (int i = 0; i < keyCount; i++) {
            result.keys.add(in.readUTF());
        }

        if (pageType == LEAF_PAGE) {
            for (int i = 0; i < keyCount; i++) {
                result.values.add(in.readLong());
            }
        } else {
            int childCount = in.readInt();

            for (int i = 0; i < childCount; i++) {
                result.childPageIds.add(in.readInt());
            }
        }

        return result;
    }
}