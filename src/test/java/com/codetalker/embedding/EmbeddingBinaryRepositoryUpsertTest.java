package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

public class EmbeddingBinaryRepositoryUpsertTest {

    @Test
    public void testUpsertSameContentHashReturnsSameId() throws Exception {
        // Use a unique in-memory DB for this test via DatabaseManager
        DatabaseManager db = new DatabaseManager("test-data-upsert");
        EmbeddingBinaryRepository repo = new EmbeddingBinaryRepository(db);

        String contentHash = "upsert-test-hash-12345";
        Embedding e1 = new Embedding();
        e1.contentHash = contentHash;
        e1.filePath = "file1.txt";
        e1.contentType = "text/plain";
        e1.chunkText = "first save";
        e1.vectorJson = "[0.1,0.2]";
        e1.metadata = "{}";

        Embedding saved1 = repo.save(e1);
        assertNotNull(saved1);
        assertNotNull(saved1.id);

        // Perform a second save with the same content hash but different payload
        Embedding e2 = new Embedding();
        e2.contentHash = contentHash;
        e2.filePath = "file2.txt";
        e2.contentType = "text/plain";
        e2.chunkText = "second save";
        e2.vectorJson = "[0.3,0.4]";
        e2.metadata = "{\"note\":\"second\"}";

        Embedding saved2 = repo.save(e2);
        assertNotNull(saved2);
        assertNotNull(saved2.id);

        // Both saves should return the same id (upsert by content_hash)
        assertEquals(saved1.id.longValue(), saved2.id.longValue(), "Upsert should return same id for identical content_hash");

        // Optionally ensure that findByHash returns a row and id matches
        Embedding lookup = repo.findByHash(contentHash);
        assertNotNull(lookup);
        assertEquals(saved1.id.longValue(), lookup.id.longValue());
    }
}
