package com.codetalker.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MetricsInfoTest {

    @Test
    public void parseRuntimePresent() throws Exception {
        String json = "{\"runtime\":\"st\",\"loaded\":true,\"error\":null,\"versions\":{\"numpy\":\"1.2.3\",\"torch\":\"2.6.0\"}}";
        MetricsInfo m = MetricsInfo.fromJson(json);
        assertEquals("st", m.runtime);
        assertTrue(m.loaded);
        assertNull(m.error);
        assertNotNull(m.versions);
        assertEquals("1.2.3", m.versions.get("numpy"));
    }

    @Test
    public void parseRuntimeNull() throws Exception {
        String json = "{\"runtime\":null,\"loaded\":false,\"error\":\"no runtime\",\"versions\":{}}";
        MetricsInfo m = MetricsInfo.fromJson(json);
        assertNull(m.runtime);
        assertFalse(m.loaded);
        assertEquals("no runtime", m.error);
        assertNotNull(m.versions);
        assertTrue(m.versions.isEmpty());
    }
}
