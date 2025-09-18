package com.codetalker.ann;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class JelmerkAnnIndexTest {

    @Test
    public void basicSmokeTest() {
        JelmerkAnnIndex idx = new JelmerkAnnIndex(3);
        idx.add("a", new float[]{1f, 0f, 0f});
        idx.add("b", new float[]{0f, 1f, 0f});
        idx.add("c", new float[]{0f, 0f, 1f});
        idx.build();

        String[] top = idx.query(new float[]{1f, 0f, 0f}, 1);
        assertEquals(1, top.length);
        assertEquals("a", top[0]);

        idx.close();
    }
}
