package com.codetalker.embedding;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.ann.JelmerkAnnIndex;
import com.codetalker.storage.DatabaseManager;

public class PersistedVectorIndexAdapterTest {
    private DatabaseManager db;

    @BeforeEach
    void setup() throws Exception {
        Path p = Path.of("./test-data-adapter");
        if (Files.exists(p)) Files.walk(p).sorted((a,b)->b.compareTo(a)).forEach(path -> { try { Files.delete(path);} catch (Exception ignored) {} });
        db = new DatabaseManager("./test-data-adapter");
    }

    @AfterEach
    void tearDown() { if (db != null) db.shutdown(); }

    @Test
    void testAdapterLoadsVectors() throws Exception {
    PersistenceEmbeddingService pes = new PersistenceEmbeddingService(db, new EmbeddingService(3));
    long idA = pes.persist("/a", 0, "A");
    long idB = pes.persist("/b", 0, "B");
    long idC = pes.persist("/c", 0, "C");

    JelmerkAnnIndex idx = new JelmerkAnnIndex(3);
    EmbeddingBinaryRepository repo = new EmbeddingBinaryRepository(db);
    PersistedVectorIndexAdapter adapter = new PersistedVectorIndexAdapter(repo, idx);
    int added = adapter.preload();
    assertEquals(3, added);

    // verify that vectors were added by checking repo contents
    assertEquals("/a", repo.findById(idA).filePath);
    }
}
