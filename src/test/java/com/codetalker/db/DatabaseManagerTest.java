package com.codetalker.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class DatabaseManagerTest {

    @Test
    public void testInitInsertAndQuery() throws Exception {
        try (DatabaseManager db = new DatabaseManager()) {
            // init schema
            db.init();
            // Ensure JDBC URL is in-memory during tests
            String url = db.getJdbcUrl();
            assertNotNull(url);

            long before = db.countEmbeddings();
            byte[] emb = new byte[] {1,2,3,4};
            long id = db.insertEmbedding("doc-1", 0, "hello world", emb);
            long after = db.countEmbeddings();
            assertEquals(before + 1, after);

            var row = db.findEmbeddingById(id);
            assertNotNull(row);
            assertEquals("doc-1", row.get("doc_id"));
            Object ebytes = row.get("embedding");
            assertNotNull(ebytes);
            assertEquals(byte[].class, ebytes.getClass());
        }
    }
}
