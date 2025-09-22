package com.codetalker.embedding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

class EmbeddingRepositoryTest {
    private DatabaseManager dbManager;
    private com.codetalker.embedding.EmbeddingStore repo;

    @BeforeEach
    void setUp() throws Exception {
        Path p = Path.of("./test-data");
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> b.compareTo(a)).forEach(path -> { try { Files.delete(path); } catch (Exception ignored) {} });
        }
    String testDbPath = "./test-data/repo-embeddings-" + System.currentTimeMillis();
    dbManager = new DatabaseManager(testDbPath);
    repo = new com.codetalker.embedding.EmbeddingBinaryRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) dbManager.shutdown();
    }

    @Test
    void testCrud() throws Exception {
        Embedding e = new Embedding();
        e.contentHash = "hash1";
        e.filePath = "/tmp/test1";
        e.contentType = "text/plain";
        e.chunkText = "hello world";
        e.vectorJson = "[0.1,0.2]";
        e.metadata = "{}";

        Embedding saved = repo.save(e);
        assertNotNull(saved.id);

        Embedding byId = repo.findById(saved.id);
        assertEquals("hash1", byId.contentHash);

        Embedding byHash = repo.findByHash("hash1");
        assertNotNull(byHash);

        List<Embedding> list = repo.listAll(10);
        assertTrue(list.size() >= 1);

        boolean deleted = repo.delete(saved.id);
        assertTrue(deleted);

        Embedding after = repo.findById(saved.id);
        assertNull(after);
    }
}
