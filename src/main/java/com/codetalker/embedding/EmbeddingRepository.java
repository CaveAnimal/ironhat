package com.codetalker.embedding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.codetalker.storage.DatabaseManager;

public class EmbeddingRepository {
    private final DatabaseManager db;

    public EmbeddingRepository(DatabaseManager db) {
        this.db = db;
    }

    public Embedding save(Embedding e) throws Exception {
        String sql = "INSERT INTO embeddings (content_hash,file_path,content_type,chunk_text,vector,metadata) VALUES (?,?,?,?,?,?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.contentHash);
            ps.setString(2, e.filePath);
            ps.setString(3, e.contentType);
            ps.setString(4, e.chunkText);
            ps.setString(5, e.vectorJson);
            ps.setString(6, e.metadata);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.id = rs.getLong(1);
            }
            return e;
        }
    }

    public Embedding findById(long id) throws Exception {
        String sql = "SELECT * FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public Embedding findByHash(String hash) throws Exception {
        String sql = "SELECT * FROM embeddings WHERE content_hash = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public List<Embedding> listAll(int limit) throws Exception {
        String sql = "SELECT * FROM embeddings ORDER BY id DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Embedding> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        }
    }

    public boolean delete(long id) throws Exception {
        String sql = "DELETE FROM embeddings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int upd = ps.executeUpdate();
            return upd > 0;
        }
    }

    private Embedding mapRow(ResultSet rs) throws Exception {
        Embedding e = new Embedding();
        e.id = rs.getLong("id");
        e.contentHash = rs.getString("content_hash");
        e.filePath = rs.getString("file_path");
        e.contentType = rs.getString("content_type");
        e.chunkText = rs.getString("chunk_text");
        e.vectorJson = rs.getString("vector");
        e.metadata = rs.getString("metadata");
        e.createdAt = rs.getTimestamp("created_at").toInstant();
        e.updatedAt = rs.getTimestamp("updated_at").toInstant();
        return e;
    }
}
