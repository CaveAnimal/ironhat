package com.codetalker.ann;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class InMemoryAnnIndexTest {

    @Test
    public void basicAddAndQuery() {
        InMemoryAnnIndex idx = new InMemoryAnnIndex();
        idx.add("a", new float[]{1f, 0f, 0f});
        idx.add("b", new float[]{0f, 1f, 0f});
        idx.add("c", new float[]{0f, 0f, 1f});
        idx.build();

        String[] top = idx.query(new float[]{1f, 0f, 0f}, 3);
        assertEquals(3, top.length);
        assertEquals("a", top[0]);
        // b and c may tie; ensure deterministic ordering
        assertTrue(("b".equals(top[1]) && "c".equals(top[2])) || ("c".equals(top[1]) && "b".equals(top[2])));

        idx.close();
    }
}
