package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Integration test skeleton for running SavedModelBundle-backed inference.
 *
 * This test is intended to be executed under the Maven profile `with-tensorflow`.
 * Activate with: `mvn -Pwith-tensorflow verify` (or `-DwithTensorFlow=true`).
 * The profile sets the property `tf.integration.enabled=true` which this test checks.
 *
 * The test expects a system property `codetalker.model.path` to point to a SavedModel
 * directory (SavedModelBundle.load(path) compatible). On CI, provide a small test
 * SavedModel compatible with the TF Java version used in the profile.
 */
public class TFSavedModelIntegrationTest {

    @Test
    public void testSavedModelBundleInference() throws Exception {
        // Skip unless the profile-enabled property is present
        boolean tfEnabled = Boolean.parseBoolean(System.getProperty("tf.integration.enabled", "false"));
        Assumptions.assumeTrue(tfEnabled, "TensorFlow integration profile not enabled; skipping integration test");

        String modelPath = System.getProperty("codetalker.model.path");
        assertNotNull(modelPath, "Provide -Dcodetalker.model.path=/path/to/saved_model for integration test");

        ModelLoader loader = new ModelLoader(modelPath);
        loader.load(); // may throw if model missing or TF not available

        // Ensure loader reports TF runtime available
        assertTrue(loader.isTensorflowRuntimeAvailable(), "SavedModelBundle or TFLite interpreter should be available when running with TensorFlow profile");

        TFEmbeddingService svc = new TFEmbeddingService(loader, 16);
        float[] emb = svc.embed("integration test");
        assertNotNull(emb);
        assertEquals(16, emb.length);
        // At least one non-zero entry expected for a real model
        boolean anyNonZero = false;
        for (float v : emb) if (v != 0.0f) { anyNonZero = true; break; }
        assertTrue(anyNonZero, "Expected non-zero embedding from the SavedModelBundle inference");
    }
}
