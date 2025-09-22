package com.codetalker.embedding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.codetalker.storage.DatabaseManager;

public class EmbeddingBinaryRepository implements EmbeddingStore {
    private final DatabaseManager db;

    public EmbeddingBinaryRepository(DatabaseManager db) {
        this.db = db;
    }

    public DatabaseManager getDb() {
        return this.db;
    }

    /**
     * Return the dimensionality of the first non-null vector found in the DB, or -1 if none.
     */
    public int detectVectorDimension() throws Exception {
        // First try to read a metadata JSON that may include a 'dimensions' field.
        String metaSql = "SELECT metadata FROM embeddings WHERE metadata IS NOT NULL LIMIT 1";
        try (java.sql.Connection conn = db.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(metaSql)) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String meta = rs.getString(1);
                    if (meta != null && !meta.isBlank()) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                            java.util.Map<?,?> m = om.readValue(meta, java.util.Map.class);
                            Object d = m.get("dimensions");
                            if (d instanceof Number) return ((Number)d).intValue();
                            if (d instanceof String) return Integer.parseInt((String)d);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        // Fallback: inspect first non-null vector blob
        String sql = "SELECT vector FROM embeddings WHERE vector IS NOT NULL LIMIT 1";
        try (java.sql.Connection conn = db.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] b = rs.getBytes(1);
                    if (b == null) return -1;
                    float[] v = FloatSerializationUtils.bytesToFloats(b);
                    return v != null ? v.length : -1;
                }
            }
        }
        return -1;
    }

    /**
     * Count number of rows that have non-null vector blobs.
     */
    public long countVectors() throws Exception {
        String sql = "SELECT COUNT(*) FROM embeddings WHERE vector IS NOT NULL";
        try (java.sql.Connection conn = db.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0L;
    }

    // legacy helper (now performs an upsert by content_hash)
    public long save(String contentHash, String filePath, String contentType, String chunkText, byte[] vector, String metadata) throws Exception {
        // Use H2 MERGE INTO which behaves like an UPSERT keyed on content_hash.
        // MERGE does not return generated keys reliably across databases, so after executing
        // we SELECT the id for the content_hash to return it.
        String mergeSql = "MERGE INTO embeddings (content_hash, file_path, content_type, chunk_text, vector, metadata) KEY(content_hash) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection()) {
            // Use a transaction boundary to ensure atomicity for the upsert + select
            boolean previousAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(mergeSql)) {
                    ps.setString(1, contentHash);
                    ps.setString(2, filePath);
                    ps.setString(3, contentType);
                    ps.setString(4, chunkText);
                    ps.setBytes(5, vector);
                    ps.setString(6, metadata);
                    ps.executeUpdate();
                }

                // Retrieve the id for the (possibly new) row
                String idSql = "SELECT id FROM embeddings WHERE content_hash = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(idSql)) {
                    ps2.setString(1, contentHash);
                    try (ResultSet rs = ps2.executeQuery()) {
                        if (rs.next()) {
                            long id = rs.getLong(1);
                            conn.commit();
                            return id;
                        }
                    }
                }

                conn.commit();
                throw new IllegalStateException("Failed to upsert or retrieve id for content_hash=" + contentHash);
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignored) {}
                throw e;
            } finally {
                try { conn.setAutoCommit(previousAutoCommit); } catch (Exception ignored) {}
            }
        }
    }

    // Implement EmbeddingStore API
    @Override
    public Embedding save(Embedding e) throws Exception {
        byte[] vec = null;
        if (e.vectorJson != null) vec = e.vectorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long id = save(e.contentHash, e.filePath, e.contentType, e.chunkText, vec, e.metadata);
        e.id = id;
        return e;
    }

    @Override
    public Embedding findById(long id) throws Exception {
        String sql = "SELECT id, content_hash, file_path, content_type, chunk_text, vector, metadata, created_at, updated_at FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    @Override
    public Embedding findByHash(String hash) throws Exception {
        String sql = "SELECT id, content_hash, file_path, content_type, chunk_text, vector, metadata, created_at, updated_at FROM embeddings WHERE content_hash = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    @Override
    public java.util.List<Embedding> listAll(int limit) throws Exception {
        String sql = "SELECT id, content_hash, file_path, content_type, chunk_text, vector, metadata, created_at, updated_at FROM embeddings ORDER BY id DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<Embedding> out = new java.util.ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        }
    }

    @Override
    public boolean delete(long id) throws Exception {
        String sql = "DELETE FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Embedding mapRow(ResultSet rs) throws Exception {
        Embedding e = new Embedding();
        e.id = rs.getLong("id");
        e.contentHash = rs.getString("content_hash");
        e.filePath = rs.getString("file_path");
        e.contentType = rs.getString("content_type");
        e.chunkText = rs.getString("chunk_text");
        byte[] b = rs.getBytes("vector");
        e.vectorJson = b == null ? null : new String(b, java.nio.charset.StandardCharsets.UTF_8);
        e.metadata = rs.getString("metadata");
        java.sql.Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) e.createdAt = ct.toInstant();
        java.sql.Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) e.updatedAt = ut.toInstant();
        return e;
    }

    /**
     * Return the raw vector bytes stored for the given id (or null if none).
     */
    public byte[] findVectorById(long id) throws Exception {
        String sql = "SELECT vector FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("vector");
                return null;
            }
        }
    }
}
