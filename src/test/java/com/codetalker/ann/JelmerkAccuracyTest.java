package com.codetalker.ann;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class JelmerkAccuracyTest {

    @Test
    public void top1ExactMatch() {
        int dim = 8;
        JelmerkAnnIndex idx = new JelmerkAnnIndex(dim, 8, 10, 100);
        float[] target = new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
        idx.add("target", target);

        // add some distractors
        idx.add("d1", new float[]{0f,1f,0f,0f,0f,0f,0f,0f});
        idx.add("d2", new float[]{0f,0f,1f,0f,0f,0f,0f,0f});

        idx.build();

        String[] top = idx.query(target, 1);
        assertEquals(1, top.length);
        assertEquals("target", top[0]);
        idx.close();
    }
}
