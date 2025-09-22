package com.codetalker.embedding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

class VectorSearchServiceTest {
    private DatabaseManager dbManager;
    private com.codetalker.embedding.EmbeddingStore repo;
    private VectorSearchService svc;

    @BeforeEach
    void setUp() throws Exception {
        Path p = Path.of("./test-data");
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> b.compareTo(a)).forEach(path -> { try { Files.delete(path); } catch (Exception ignored) {} });
        }
        String testDbPath = "./test-data/search-embeddings-" + System.currentTimeMillis();
        dbManager = new DatabaseManager(testDbPath);
    repo = new com.codetalker.embedding.EmbeddingBinaryRepository(dbManager);
        svc = new VectorSearchService(repo);

        // seed with three simple 3-d vectors
        Embedding e1 = new Embedding(); e1.contentHash = "a"; e1.vectorJson = "[1.0,0.0,0.0]"; e1.filePath="/a"; e1.contentType="text"; e1.chunkText="A"; e1.metadata="{}"; repo.save(e1);
        Embedding e2 = new Embedding(); e2.contentHash = "b"; e2.vectorJson = "[0.0,1.0,0.0]"; e2.filePath="/b"; e2.contentType="text"; e2.chunkText="B"; e2.metadata="{}"; repo.save(e2);
        Embedding e3 = new Embedding(); e3.contentHash = "c"; e3.vectorJson = "[0.0,0.0,1.0]"; e3.filePath="/c"; e3.contentType="text"; e3.chunkText="C"; e3.metadata="{}"; repo.save(e3);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) dbManager.shutdown();
    }

    @Test
    void testSearchNearest() throws Exception {
        float[] query = new float[]{1.0f, 0.0f, 0.0f};
        List<VectorSearchService.Hit> hits = svc.search(query, 3);
        assertEquals(3, hits.size());
        assertEquals("a", hits.get(0).embedding.contentHash);
        assertEquals("b", hits.get(1).embedding.contentHash);
        assertEquals("c", hits.get(2).embedding.contentHash);
    }

    @Test
    void testTopK() throws Exception {
        float[] query = new float[]{0.0f, 1.0f, 0.0f};
        List<VectorSearchService.Hit> hits = svc.search(query, 1);
        assertEquals(1, hits.size());
        assertEquals("b", hits.get(0).embedding.contentHash);
    }
}
