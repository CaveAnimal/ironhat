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
        // Run Flyway migrations against this DataSource so the schema is versioned and repeatable
        try {
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure().dataSource(ds).load();
            flyway.migrate();
        } catch (Exception e) {
            logger.warn("Flyway migration failed or not applicable: {}", e.getMessage());
        }
        return ds;
    }

    private void initializeSchema() {
        // Schema initialization is now handled via Flyway migrations run during DataSource setup.
        logger.info("Schema initialization delegated to Flyway migrations");
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
