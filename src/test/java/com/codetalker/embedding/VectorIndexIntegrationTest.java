package com.codetalker.embedding;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.ann.JelmerkAnnIndex;
import com.codetalker.storage.DatabaseManager;

class VectorIndexIntegrationTest {
    private DatabaseManager db;
    private PersistenceEmbeddingService pes;
    private java.nio.file.Path tmpDir;

    @BeforeEach
    void setup() throws Exception {
        // create a unique temporary directory for each test to avoid cross-test file-based DB clashes
        tmpDir = java.nio.file.Files.createTempDirectory("test-db-index-");
        // use a relative path under the project so Flyway/H2 file URLs behave consistently
        String dbPath = tmpDir.toAbsolutePath().toString() + java.io.File.separator + "test-data-index";
        db = new DatabaseManager(dbPath);
        EmbeddingService generator = new EmbeddingService(8);
        pes = new PersistenceEmbeddingService(db, generator);
    }

    @AfterEach
    void tearDown() {
        if (db != null) db.shutdown();
        // best-effort cleanup of temp directory
        if (tmpDir != null) {
            try {
                java.nio.file.Files.walk(tmpDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(java.io.File::delete);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void testIndexPreloadAndQuery() throws Exception {
        // persist a few embeddings with deterministic generator
        long id1 = pes.persist("doc1", 0, "apple");
        long id2 = pes.persist("doc2", 0, "banana");
        long id3 = pes.persist("doc3", 0, "carrot");

        // build an ann index with the same dimensionality
        JelmerkAnnIndex index = new JelmerkAnnIndex(8);
        EmbeddingBinaryRepository repo = new EmbeddingBinaryRepository(db);
        PersistedVectorIndexAdapter adapter = new PersistedVectorIndexAdapter(repo, index);
        adapter.preload(Math.max(id1, Math.max(id2, id3)));

        // query using generator's vector for 'banana' and expect closest is id2
        EmbeddingService gen = new EmbeddingService(8);
        float[] q = gen.embed("banana");
        String[] ids = index.query(q, 3);
        assertEquals(String.valueOf(id2), ids[0]);
    }

}
