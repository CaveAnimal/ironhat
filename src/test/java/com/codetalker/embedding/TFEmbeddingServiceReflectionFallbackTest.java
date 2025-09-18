package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Ensures TFEmbeddingService increments its reflectionFallbackCount when
 * reflective TF runtime methods are missing or fail.
 */
public class TFEmbeddingServiceReflectionFallbackTest {

    @Test
    public void reflectionFallbackCountIncrementsWhenNoTFMethods() {
        ModelLoader loader = new ModelLoader();
        // inject a plain Object as the SavedModelBundle instance so there are no
        // expected methods like run/session/runner present.
        loader.setSavedModelBundleInstance(new Object());

        TFEmbeddingService svc = new TFEmbeddingService(loader, 3);

        int before = svc.getReflectionFallbackCount();
        float[] out = svc.embed("hello world");
        int after = svc.getReflectionFallbackCount();

        // service should have used fallback embeddings (same length as configured dim)
        assertEquals(3, out.length, "embedding length should match configured dim");
        // reflection fallback count should increase by at least 1
        assertEquals(before + 1, after, "reflectionFallbackCount should increment when reflection fails");
    }

}
