package com.mujah.minidb.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConfigTest {

    @Test
    void shouldStoreValues() {
        ServerConfig config =
                new ServerConfig(6380, "pw", "test.db");

        assertEquals(6380, config.port());
        assertEquals("pw", config.password());
        assertEquals("test.db", config.dataFile());
    }
}