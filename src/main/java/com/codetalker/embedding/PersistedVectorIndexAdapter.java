package com.codetalker.embedding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.codetalker.ann.JelmerkAnnIndex;

/**
 * Adapter that loads persisted vectors from the DB and populates a JelmerkAnnIndex.
 */
public class PersistedVectorIndexAdapter {
    private final EmbeddingBinaryRepository repo;
    private final JelmerkAnnIndex index;
    private final int batchSize;
    private final int fetchSize;

    public PersistedVectorIndexAdapter(EmbeddingBinaryRepository repo, JelmerkAnnIndex index) {
        this(repo, index, 1000, 0);
    }

    public PersistedVectorIndexAdapter(EmbeddingBinaryRepository repo, JelmerkAnnIndex index, int batchSize, int fetchSize) {
        this.repo = repo;
        this.index = index;
        this.batchSize = batchSize <= 0 ? 1000 : batchSize;
        this.fetchSize = fetchSize; // 0 == default, -1 == driver-specific streaming
    }

    /**
     * Stream vectors from the DB (SELECT id, vector FROM embeddings WHERE vector IS NOT NULL) and add them into the index.
     * Returns the number of items added.
     */
    public int preload() throws Exception {
        long lastId = 0L;
        AtomicInteger count = new AtomicInteger(0);
        String sql = "SELECT id, vector FROM embeddings WHERE id > ? AND vector IS NOT NULL ORDER BY id ASC LIMIT ?";
        try (Connection conn = repo.getDb().getConnection()) {
            // Driver-specific tuning
            try {
                String driver = repo.getDb().getDriverName();
                if ("postgresql".equals(driver)) {
                    // For Postgres streaming large results: disable auto-commit
                    conn.setAutoCommit(false);
                }
            } catch (Exception ignored) {
                // best-effort tuning
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Apply fetch size hints where supported
                try {
                    String driver = repo.getDb().getDriverName();
                    if (fetchSize != 0) {
                        ps.setFetchSize(fetchSize);
                    } else if ("postgresql".equals(driver)) {
                        ps.setFetchSize(Math.max(50, Math.min(1000, batchSize)));
                    } else if ("mysql".equals(driver)) {
                        // MySQL: enable result set streaming
                        ps.setFetchSize(Integer.MIN_VALUE);
                    } else if ("h2".equals(driver)) {
                        // H2: set a modest fetch size if supported
                        ps.setFetchSize(Math.max(100, Math.min(1000, batchSize)));
                    }
                } catch (Exception ignored) {
                    // ignore tuning failures
                }

                while (true) {
                    ps.setLong(1, lastId);
                    ps.setInt(2, batchSize);
                    try (ResultSet rs = ps.executeQuery()) {
                        int batchCount = 0;
                        while (rs.next()) {
                            long id = rs.getLong(1);
                            byte[] b = rs.getBytes(2);
                            if (b == null) continue;
                            float[] vec = FloatSerializationUtils.bytesToFloats(b);
                            index.add(String.valueOf(id), vec);
                            count.incrementAndGet();
                            batchCount++;
                            lastId = id;
                        }
                        if (batchCount == 0) break;
                        if (batchCount < batchSize) break;
                    }
                }
            }
        }
        index.build();
        return count.get();
    }

    /**
     * Backward-compatible preload by scanning ids 1..maxId (kept for compatibility but less efficient).
     */
    public void preload(long maxId) throws Exception {
        for (long id = 1; id <= maxId; id++) {
            byte[] b = repo.findVectorById(id);
            if (b == null) continue;
            float[] vec = FloatSerializationUtils.bytesToFloats(b);
            index.add(String.valueOf(id), vec);
        }
        index.build();
    }
}
