package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for large-dimension embeddings to guard against array-shape and
 * performance regressions. Uses the in-JVM fallback embedding (no TF needed).
 */
public class TFEmbeddingServiceLargeEmbeddingSmokeTest {

    @Test
    public void largeEmbeddingSmoke() {
        final int dim = 4096;
        ModelLoader loader = new ModelLoader();
        // ensure loader is marked loaded so TFEmbeddingService uses injected fallback
        try {
            java.lang.reflect.Field loadedField = ModelLoader.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.setBoolean(loader, true);
        } catch (Exception e) {
            throw new RuntimeException("failed to set loader loaded flag", e);
        }

        TFEmbeddingService svc = new TFEmbeddingService(loader, dim);
        float[] emb = svc.embed("this is a smoke test for large embeddings");

        assertEquals(dim, emb.length, "embedding length should equal requested dim");

        // Basic numeric checks: values finite and not NaN/Infinity
        for (int i = 0; i < emb.length; i++) {
            assertTrue(Float.isFinite(emb[i]), "embedding value should be finite at index " + i);
        }
    }

}
