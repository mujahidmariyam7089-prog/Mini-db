package com.mujah.minidb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodecTest {

    private final Codec codec = new Codec();

    @Test
    void encodesAndDecodesNormalStrings() {
        List<String> input = List.of("Hello", "World");

        String encoded = codec.encode(input);
        List<String> decoded = codec.decode(encoded);

        assertEquals(input, decoded);
    }

    @Test
    void handlesEmptyString() {
        List<String> input = List.of("");

        String encoded = codec.encode(input);
        List<String> decoded = codec.decode(encoded);

        assertEquals(input, decoded);
    }

    @Test
    void handlesDelimiterInsideString() {
        List<String> input = List.of("a#b", "c");

        String encoded = codec.encode(input);
        List<String> decoded = codec.decode(encoded);

        assertEquals(input, decoded);
    }

    @Test
    void handlesEmptyList() {
        List<String> input = List.of();

        String encoded = codec.encode(input);
        List<String> decoded = codec.decode(encoded);

        assertEquals(input, decoded);
    }
}