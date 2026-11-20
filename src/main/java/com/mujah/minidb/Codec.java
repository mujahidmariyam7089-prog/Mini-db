package com.mujah.minidb;

import java.util.ArrayList;
import java.util.List;

public class Codec {

    public String encode(List<String> strs) {
        StringBuilder encoded = new StringBuilder();

        for (String s : strs) {
            encoded.append(s.length())
                   .append('#')
                   .append(s);
        }

        return encoded.toString();
    }

    public List<String> decode(String s) {
        List<String> decoded = new ArrayList<>();
        int i = 0;

        while (i < s.length()) {
            int delimiter = s.indexOf('#', i);
            int length = Integer.parseInt(s.substring(i, delimiter));

            int start = delimiter + 1;
            decoded.add(s.substring(start, start + length));

            i = start + length;
        }

        return decoded;
    }
}