package com.codetalker.storage;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    private static final String TEST_DB_PATH = "./test-data/test-embeddings";
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up test data
        Path p = Path.of("./test-data");
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try { Files.delete(path); } catch (Exception ignored) {}
            });
        }

        dbManager = new DatabaseManager(TEST_DB_PATH);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) dbManager.shutdown();
    }

    @Test
    void testConnectionAndSchema() throws Exception {
        assertNotNull(dbManager.getDataSource());

        DatabaseManager.DatabaseStats stats = dbManager.getStats();
        assertNotNull(stats);
        assertTrue(stats.totalEmbeddings >= 0);
    }
}
