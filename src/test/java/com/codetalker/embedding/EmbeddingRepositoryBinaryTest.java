package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

class EmbeddingRepositoryBinaryTest {
    private DatabaseManager db;
    private EmbeddingBinaryRepository repo;

    @BeforeEach
    void setup() throws Exception {
        Path p = Path.of("./test-data-binary");
        if (Files.exists(p)) Files.walk(p).sorted((a,b)->b.compareTo(a)).forEach(path -> { try { Files.delete(path);} catch (Exception ignored) {} });
        db = new DatabaseManager("./test-data-binary");
        repo = new EmbeddingBinaryRepository(db);
    }

    @AfterEach
    void tearDown() {
        if (db != null) db.shutdown();
    }

    @Test
    void testSaveAndLoadVector() throws Exception {
        byte[] vec = new byte[] {10,20,30,40};
        long id = repo.save("h1", "/f", "text", "chunk", vec, "{}");
        byte[] got = repo.findVectorById(id);
        assertArrayEquals(vec, got);
        assertNotEquals(0L, id);
    }
}
