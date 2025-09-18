package com.codetalker.embedding;

import org.junit.jupiter.api.Test;

/**
 * Ensures TFEmbeddingService increments its reflectionFallbackCount when
 * a found reflective method throws during invocation (InvocationTargetException).
 */
public class TFEmbeddingServiceInvocationExceptionTest {

    // A fake interpreter-like object whose run(in,out) throws an unchecked exception.
    public static class ThrowingInterpreter {
        public void run(Object in, Object out) {
            throw new RuntimeException("simulated failure in run");
        }
    }

    @Test
    public void reflectionFallbackCountIncrementsOnInvocationException() {
        ModelLoader loader = new ModelLoader();
        // inject our throwing interpreter as the TFLite interpreter instance
        // (ModelLoader holds interpreterInstance as package-private; we use reflection to set it)
        try {
            java.lang.reflect.Field f = ModelLoader.class.getDeclaredField("interpreterInstance");
            f.setAccessible(true);
            f.set(loader, new ThrowingInterpreter());
            // mark the loader as loaded so TFEmbeddingService will attempt to use the interpreter
            java.lang.reflect.Field loadedField = ModelLoader.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.setBoolean(loader, true);
        } catch (Exception e) {
            throw new RuntimeException("failed to set up test loader", e);
        }

        TFEmbeddingService svc = new TFEmbeddingService(loader, 4);

        int before = svc.getReflectionFallbackCount();
        float[] out = svc.embed("test invocation exception");
        int after = svc.getReflectionFallbackCount();

    // fallback should produce array of configured dim
    org.junit.jupiter.api.Assertions.assertEquals(4, out.length, "embedding length should match configured dim");
    // The service may increment the counter multiple times as it tries multiple
    // reflective paths; assert it increased by at least 1 to prove the invocation
    // failure triggered the fallback telemetry.
    org.junit.jupiter.api.Assertions.assertTrue(after >= before + 1,
        "reflectionFallbackCount should increment when invocation throws");
    }

}
