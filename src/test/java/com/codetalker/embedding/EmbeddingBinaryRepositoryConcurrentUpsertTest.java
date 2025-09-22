package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

/**
 * Minimal concurrency test that attempts to exercise the upsert path by saving the same
 * content_hash from multiple threads concurrently. The repository should return the
 * same id for all successful saves and not throw fatal exceptions.
 */
public class EmbeddingBinaryRepositoryConcurrentUpsertTest {

    @Test
    public void testConcurrentUpsertReturnsSameId() throws Exception {
        DatabaseManager db = new DatabaseManager("test-data-upsert-concurrent");
        EmbeddingBinaryRepository repo = new EmbeddingBinaryRepository(db);

        final String contentHash = "concurrent-upsert-hash-xyz";
        int threads = 8;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        List<Callable<Long>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            tasks.add(() -> {
                Embedding e = new Embedding();
                e.contentHash = contentHash;
                e.filePath = "file-concurrent-" + idx + ".txt";
                e.contentType = "text/plain";
                e.chunkText = "concurrent save " + idx;
                e.vectorJson = "[" + idx + "," + (idx + 0.1) + "]";
                e.metadata = "{\"thread\":" + idx + "}";
                try {
                    Embedding saved = repo.save(e);
                    if (saved == null || saved.id == null) throw new IllegalStateException("save returned null id");
                    return saved.id;
                } catch (Exception exn) {
                    // Wrap as runtime to let the future capture it
                    throw new RuntimeException(exn);
                }
            });
        }

        List<Future<Long>> futures = ex.invokeAll(tasks);
        ex.shutdown();
        ex.awaitTermination(10, TimeUnit.SECONDS);

        Long firstId = null;
        List<Long> ids = new ArrayList<>();
        for (Future<Long> f : futures) {
            Long id = f.get();
            assertNotNull(id, "Thread returned null id");
            ids.add(id);
            if (firstId == null) firstId = id;
        }

        // All ids should be equal
        for (Long id : ids) assertEquals(firstId.longValue(), id.longValue());

        // And lookup by hash should return the same id
        Embedding lookup = repo.findByHash(contentHash);
        assertNotNull(lookup);
        assertEquals(firstId.longValue(), lookup.id.longValue());
    }
}
