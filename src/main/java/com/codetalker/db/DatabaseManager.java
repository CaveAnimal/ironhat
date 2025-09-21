package com.codetalker.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very small DatabaseManager that initializes an H2 schema and provides
 * basic CRUD helpers for embedding records. Tests run against an in-memory
 * H2 instance when the `test.h2.inmemory` system property is set to true
 * (the project's `pom.xml` sets this for unit tests).
 */
public class DatabaseManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final String jdbcUrl;
    private final String user = "sa";
    private final String password = "";
    private Connection conn;

    public DatabaseManager() {
        String inMem = System.getProperty("test.h2.inmemory");
        if (inMem != null && inMem.equalsIgnoreCase("true")) {
            this.jdbcUrl = "jdbc:h2:mem:codetalker;DB_CLOSE_DELAY=-1";
        } else {
            // File-based DB for local development
            this.jdbcUrl = "jdbc:h2:file:./data/ironhat-db;AUTO_SERVER=TRUE";
        }
    }

    // Visible for tests
    public String getJdbcUrl() { return jdbcUrl; }

    private Connection openConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(jdbcUrl, user, password);
        }
        return conn;
    }

    /** Initialize the DB schema from classpath resource `db/schema.sql`. */
    public void init() throws SQLException {
        try (Connection c = openConnection()) {
            c.setAutoCommit(false);
            String sql = loadResourceSql("/db/schema.sql");
            try (Statement st = c.createStatement()) {
                for (String stmt : sql.split(";")) {
                    String s = stmt.trim();
                    if (!s.isEmpty()) st.execute(s);
                }
            }
            c.commit();
        } catch (Exception e) {
            logger.warn("Failed to initialize DB schema", e);
            throw new SQLException("Schema init failed", e);
        }
    }

    private String loadResourceSql(String resourcePath) throws Exception {
        InputStream is = DatabaseManager.class.getResourceAsStream(resourcePath);
        if (is == null) throw new IllegalStateException("Resource not found: " + resourcePath);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    /** Insert an embedding record; embeddingBytes is the serialized vector (float32 LE) or JSON bytes. */
    public long insertEmbedding(String docId, int chunkIndex, String text, byte[] embeddingBytes) throws SQLException {
        String insert = "INSERT INTO embeddings(doc_id, chunk_index, text_content, embedding, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, docId);
            ps.setInt(2, chunkIndex);
            ps.setString(3, text);
            ps.setBytes(4, embeddingBytes);
            ps.setObject(5, Instant.now());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Insert failed, no rows affected");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Insert failed to produce generated id");
    }

    public long countEmbeddings() throws SQLException {
        String q = "SELECT COUNT(*) FROM embeddings";
        try (Connection c = openConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(q)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    public Map<String, Object> findEmbeddingById(long id) throws SQLException {
        String q = "SELECT id, doc_id, chunk_index, text_content, embedding, created_at FROM embeddings WHERE id = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("doc_id", rs.getString("doc_id"));
                    m.put("chunk_index", rs.getInt("chunk_index"));
                    m.put("text_content", rs.getString("text_content"));
                    m.put("embedding", rs.getBytes("embedding"));
                    m.put("created_at", rs.getTimestamp("created_at"));
                    return m;
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
