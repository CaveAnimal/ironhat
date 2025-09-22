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
    private final String jdbcUrl;

    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseManager(String databasePath) {
        this.databasePath = databasePath;
        this.dataSource = initializeDataSource(databasePath);
        this.jdbcUrl = computeJdbcUrl(databasePath);
        initializeSchema();
    }

    private DataSource initializeDataSource(String databasePath) {
        JdbcDataSource ds = new JdbcDataSource();
        String jdbc = computeJdbcUrl(databasePath);
        ds.setURL(jdbc);
        ds.setUser(DB_USER);
        ds.setPassword(DB_PASSWORD);
    logger.info("Configured H2 DataSource with URL {} (requested path={})", jdbc, databasePath);
        // Run Flyway migrations against this DataSource so the schema is versioned and repeatable
        try {
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure().dataSource(ds).load();
            flyway.migrate();
        } catch (Exception e) {
            logger.warn("Flyway migration failed or not applicable: {}", e.getMessage());
        }
        return ds;
    }

    private String computeJdbcUrl(String databasePath) {
        String useInMemory = System.getProperty("test.h2.inmemory");
        if ("true".equalsIgnoreCase(useInMemory)) {
            String safeName = databasePath == null ? "testdb" : databasePath.replaceAll("[^A-Za-z0-9]", "_");
            String unique = java.util.UUID.randomUUID().toString().replaceAll("-", "");
            return String.format("jdbc:h2:mem:%s_%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=REGULAR", safeName, unique);
        } else {
            return String.format("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=REGULAR", databasePath);
        }
    }

    /**
     * Return the JDBC URL used to configure the DataSource. Useful for driver/runtime detection.
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Try to detect the JDBC driver name from the configured DataSource URL.
     */
    public String getDriverName() {
        // First try JDBC metadata if a live connection is possible
        try (Connection c = getConnection()) {
            try {
                String product = c.getMetaData().getDatabaseProductName();
                if (product == null) product = "";
                product = product.toLowerCase();
                if (product.contains("postgres")) return "postgresql";
                if (product.contains("mysql")) return "mysql";
                if (product.contains("h2")) return "h2";
            } catch (Exception ignored) {
                // fall back to URL-based detection below
            }
        } catch (Exception ignored) {
            // cannot open connection for metadata; fall back to URL detection
        }

        if (jdbcUrl == null) return "unknown";
        String url = jdbcUrl.toLowerCase();
        if (url.startsWith("jdbc:postgresql:")) return "postgresql";
        if (url.startsWith("jdbc:mysql:")) return "mysql";
        if (url.startsWith("jdbc:h2:")) return "h2";
        return "unknown";
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
