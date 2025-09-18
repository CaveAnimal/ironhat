package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class TFEmbeddingServiceSavedModelTest {

    @Test
    public void testSavedModelBundlePathUsedWhenAvailable() throws Exception {
        // ModelLoader allows injecting instances via test helpers in the main codebase.
        ModelLoader loader = new ModelLoader();
        // create a test stub SavedModelBundle with a direct 'run' method defined in test stubs
        // Our test-only stub (src/test/java/org/tensorflow/SavedModelBundle.java) provides static load,
        // but we will create an instance of the stub and inject it reflectively.

        // Use the test stub class
        Class<?> smbCls = Class.forName("org.tensorflow.SavedModelBundle");
        Object smbInstance = smbCls.getDeclaredConstructor().newInstance();

    // inject into loader via test helper setter to avoid triggering loader.load()
    loader.setSavedModelBundleInstance(smbInstance);

        TFEmbeddingService svc = new TFEmbeddingService(loader, 16);
        float[] emb = svc.embed("hello world");
        assertNotNull(emb);
        assertEquals(16, emb.length);
        // The test stub sets output to a deterministic pattern; we just assert it's not all zeros
        boolean allZero = true;
        for (float v : emb) {
            if (v != 0.0f) { allZero = false; break; }
        }
        assertFalse(allZero, "Expected non-zero embedding from stub SavedModelBundle run");
    }
}
