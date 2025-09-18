package com.codetalker.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * EmbeddingService produces deterministic float[] embeddings for input text.
 * Uses SHA-256 of the input to derive a stable vector suitable for tests.
 */
public class EmbeddingService {
    private final int dim;

    public EmbeddingService() {
        this(8);
    }

    public EmbeddingService(int dim) {
        this.dim = dim;
    }

    // Backwards-compatible constructor used by some existing callers (e.g., SearchSmokeTest)
    public EmbeddingService(ModelLoader loader, int dim) {
        this(dim);
        // loader may influence future TF-backed implementations; currently unused
    }

    public int getDim() { return dim; }

    public float[] embed(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            float[] out = new float[dim];
            for (int i = 0; i < dim; i++) {
                out[i] = (digest[i % digest.length] & 0xff) / 255.0f;
            }
            return out;
        } catch (Exception ex) {
            int seed = text == null ? 0 : text.hashCode();
            float[] out = new float[dim];
            for (int i = 0; i < dim; i++) out[i] = ((seed >> (i % 16)) & 0xff) / 255.0f;
            return out;
        }
    }

    @Override
    public String toString() {
        return "EmbeddingService(dim=" + dim + ")";
    }
}
