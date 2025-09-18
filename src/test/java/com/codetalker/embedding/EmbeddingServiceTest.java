package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class EmbeddingServiceTest {

    @Test
    void embedHasCorrectDimensionAndDeterministic() {
        EmbeddingService svc = new EmbeddingService(16);
        float[] a = svc.embed("hello world");
        float[] b = svc.embed("hello world");
        assertNotNull(a);
        assertEquals(16, a.length);
        assertArrayEquals(a, b, 1e-6f);
    }

    @Test
    void testPlaceholderEmbeddingDeterministic() {
        EmbeddingService svc = new EmbeddingService(8);
        float[] v1 = svc.embed("hello world");
        float[] v2 = svc.embed("hello world");
        assertNotNull(v1);
        assertEquals(8, v1.length);
        for (int i = 0; i < v1.length; i++) {
            assertEquals(v1[i], v2[i], 1e-6f);
        }
    }

    @Test
    void testDifferentInputsProduceDifferentEmbeddings() {
        EmbeddingService svc = new EmbeddingService(8);
        float[] a = svc.embed("one");
        float[] b = svc.embed("two");
        boolean allEqual = true;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-6f) { allEqual = false; break; }
        }
        assertFalse(allEqual, "Different inputs should produce different placeholder embeddings");
    }
}
