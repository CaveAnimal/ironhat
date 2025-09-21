package com.codetalker.embedding;

import com.codetalker.storage.DatabaseManager;

public class PersistenceEmbeddingService {
    private final EmbeddingBinaryRepository repo;
    private final EmbeddingService generator;

    public PersistenceEmbeddingService(DatabaseManager db, EmbeddingService generator) {
        this.repo = new EmbeddingBinaryRepository(db);
        this.generator = generator;
    }

    public long persist(String docId, int chunkIndex, String text) throws Exception {
        float[] vec = generator.embed(text);
        byte[] bytes = FloatSerializationUtils.floatsToBytes(vec);
        String hash = Integer.toHexString(text.hashCode());
        return repo.save(hash, docId, "text/plain", text, bytes, "{}");
    }

    public float[] loadVector(long id) throws Exception {
        byte[] bytes = repo.findVectorById(id);
        if (bytes == null) return null;
        return FloatSerializationUtils.bytesToFloats(bytes);
    }
}
