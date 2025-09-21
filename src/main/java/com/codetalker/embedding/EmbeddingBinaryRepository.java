package com.codetalker.embedding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.codetalker.storage.DatabaseManager;

public class EmbeddingBinaryRepository {
    private final DatabaseManager db;

    public EmbeddingBinaryRepository(DatabaseManager db) {
        this.db = db;
    }

    public long save(String contentHash, String filePath, String contentType, String chunkText, byte[] vector, String metadata) throws Exception {
        String sql = "INSERT INTO embeddings (content_hash, file_path, content_type, chunk_text, vector, metadata) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, contentHash);
            ps.setString(2, filePath);
            ps.setString(3, contentType);
            ps.setString(4, chunkText);
            ps.setBytes(5, vector);
            ps.setString(6, metadata);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new IllegalStateException("Failed to insert");
        }
    }

    public byte[] findVectorById(long id) throws Exception {
        String sql = "SELECT vector FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes(1);
                return null;
            }
        }
    }
}
