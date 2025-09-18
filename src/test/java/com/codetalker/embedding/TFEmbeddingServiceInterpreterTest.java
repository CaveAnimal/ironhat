package com.codetalker.embedding;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TFEmbeddingServiceInterpreterTest {

    public static class FakeInterpreter {
        // simple run(Object in, Object out) that copies in->out
        public void run(Object in, Object out) {
            float[][] a = (float[][]) in;
            float[][] b = (float[][]) out;
            for (int i = 0; i < a[0].length && i < b[0].length; i++) b[0][i] = a[0][i] + 0.5f; // modify output so we can detect
        }
    }

    @Test
    public void usesInterpreterWhenPresent() throws Exception {
        // Ensure not in mock mode
        System.clearProperty("codetalker.model.mock");

        ModelLoader loader = new ModelLoader();
        // inject fake interpreter via reflection into loader's private field
        FakeInterpreter fi = new FakeInterpreter();
        Field f = ModelLoader.class.getDeclaredField("interpreterInstance");
        f.setAccessible(true);
        f.set(loader, fi);

        // mark as loaded so TFEmbeddingService won't try to load from disk
        Field fp = ModelLoader.class.getDeclaredField("loaded");
        fp.setAccessible(true);
        fp.set(loader, true);

        EmbeddingService fallback = new EmbeddingService(4);
        TFEmbeddingService svc = new TFEmbeddingService(loader, fallback);

        float[] res = svc.embed("abc");
        float[] base = fallback.embed("abc");

        // Interpreter adds +0.5 to each dimension in fake run
        for (int i = 0; i < base.length; i++) {
            assertEquals(base[i] + 0.5f, res[i], 1e-6);
        }
    }
}
