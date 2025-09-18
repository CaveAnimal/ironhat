package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

public class TFEmbeddingServiceTest {

    @Test
    public void fallbackUsedInMockMode() {
        System.setProperty("codetalker.model.mock", "true");
        try {
            ModelLoader loader = new ModelLoader();
            EmbeddingService fallback = new EmbeddingService(4);
            TFEmbeddingService svc = new TFEmbeddingService(loader, fallback);
            float[] a = svc.embed("hello");
            float[] b = fallback.embed("hello");
            assertArrayEquals(b, a, "TFEmbeddingService should delegate to fallback in mock mode");
        } finally {
            System.clearProperty("codetalker.model.mock");
        }
    }
}
