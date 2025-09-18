package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.codetalker.ann.JelmerkAnnIndex;
import com.codetalker.storage.DatabaseManager;

public class VectorSearchServiceJelmerkIntegrationTest {

    @Test
    public void integrationSmoke() throws Exception {
    DatabaseManager db = new DatabaseManager("./test-data/integration-embeddings");
        EmbeddingRepository repo = new EmbeddingRepository(db);

        // create and persist a few embeddings
    Embedding a = new Embedding();
    a.contentHash = "ha";
    a.filePath = "/tmp/a.txt";
    a.contentType = "text/plain";
    a.chunkText = "a";
    a.vectorJson = "[1.0,0.0,0.0]";
    a.metadata = "{}";
    repo.save(a);

    Embedding b = new Embedding();
    b.contentHash = "hb";
    b.filePath = "/tmp/b.txt";
    b.contentType = "text/plain";
    b.chunkText = "b";
    b.vectorJson = "[0.0,1.0,0.0]";
    b.metadata = "{}";
    repo.save(b);

        // create Jelmerk index and wire into service
        JelmerkAnnIndex idx = new JelmerkAnnIndex(3, 8, 10, 100);
        VectorSearchService svc = new VectorSearchService(repo, idx);

        float[] q = new float[]{1f,0f,0f};
        assertFalse(svc.search(q, 1).isEmpty());

        idx.close();
    }
}
