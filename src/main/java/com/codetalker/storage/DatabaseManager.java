package com.codetalker.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages H2 database connections and initialization
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DEFAULT_DB_PATH = "./data/embeddings";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private final DataSource dataSource;
    private final String databasePath;

    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseManager(String databasePath) {
        this.databasePath = databasePath;
        this.dataSource = initializeDataSource(databasePath);
        initializeSchema();
    }

    private DataSource initializeDataSource(String databasePath) {
        JdbcDataSource ds = new JdbcDataSource();
        String useInMemory = System.getProperty("test.h2.inmemory");
        String jdbcUrl;
        if ("true".equalsIgnoreCase(useInMemory)) {
            // Use a unique in-memory database name per DatabaseManager instance to avoid test cross-talk
            String safeName = databasePath == null ? "testdb" : databasePath.replaceAll("[^A-Za-z0-9]", "_");
            String unique = java.util.UUID.randomUUID().toString().replaceAll("-", "");
            jdbcUrl = String.format("jdbc:h2:mem:%s_%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=REGULAR", safeName, unique);
        } else {
            jdbcUrl = String.format("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=REGULAR", databasePath);
        }
        ds.setURL(jdbcUrl);
        ds.setUser(DB_USER);
        ds.setPassword(DB_PASSWORD);
        logger.info("Configured H2 DataSource with URL {} (requested path={})", jdbcUrl, databasePath);
        return ds;
    }

    private void initializeSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            StringBuilder createTable = new StringBuilder();
            createTable.append("CREATE TABLE IF NOT EXISTS embeddings (");
            createTable.append(" id BIGINT PRIMARY KEY AUTO_INCREMENT,");
            createTable.append(" content_hash VARCHAR(64) UNIQUE NOT NULL,");
            createTable.append(" file_path VARCHAR(500) NOT NULL,");
            createTable.append(" content_type VARCHAR(50) NOT NULL,");
            createTable.append(" chunk_text CLOB NOT NULL,");
            // H2 doesn't support untyped ARRAY in CREATE TABLE; store vectors as CLOB (JSON) for portability
            createTable.append(" vector CLOB,");
            createTable.append(" metadata CLOB,");
            createTable.append(" created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,");
            createTable.append(" updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            createTable.append(" );");

            stmt.execute(createTable.toString());

            // Create indexes separately to avoid executing multiple statements in a single execute call
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_file_path ON embeddings(file_path);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_content_type ON embeddings(content_type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_embeddings_hash ON embeddings(content_hash);");

            logger.info("Database schema initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void shutdown() {
        logger.info("DatabaseManager shutdown called");
        // H2 will auto-close when JVM exits; explicit shutdown can be implemented if desired
    }

    public DatabaseStats getStats() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) as total FROM embeddings");
            long total = 0;
            if (rs.next()) total = rs.getLong("total");
            return new DatabaseStats(total, databasePath);
        } catch (SQLException e) {
            logger.error("Failed to get stats", e);
            return new DatabaseStats(0, databasePath);
        }
    }

    public static class DatabaseStats {
        public final long totalEmbeddings;
        public final String databasePath;

        public DatabaseStats(long totalEmbeddings, String databasePath) {
            this.totalEmbeddings = totalEmbeddings;
            this.databasePath = databasePath;
        }
    }
}
