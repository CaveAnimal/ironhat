package com.codetalker.embedding;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codetalker.storage.DatabaseManager;

public class IndexManagerHttpIntegrationTest {
    private DatabaseManager db;
    private Thread serverThread;

    @BeforeEach
    void setup() throws Exception {
        Path p = Path.of("./test-data-indexmgr");
        if (Files.exists(p)) Files.walk(p).sorted((a,b)->b.compareTo(a)).forEach(path -> { try { Files.delete(path);} catch (Exception ignored) {} });
        db = new DatabaseManager("./test-data-indexmgr");
    }

    @AfterEach
    void teardown() throws Exception {
        if (serverThread != null && serverThread.isAlive()) serverThread.interrupt();
        if (db != null) db.shutdown();
    }

    @Test
    void testHttpRebuildCreatesSnapshot() throws Exception {
        Path snapshot = Path.of("./test-data-indexmgr/index.snap");
        // start IndexManager in background on port 8111
        serverThread = new Thread(() -> {
            try {
                IndexManager.main(new String[] {"--db", "./test-data-indexmgr", "--out", snapshot.toString(), "--http", "8111"});
            } catch (Exception e) {
                // ignore in test
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // wait a moment for server to start
        Thread.sleep(500);

        // trigger rebuild via HTTP
        URL u = new URL("http://localhost:8111/rebuild");
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("GET");
        c.connect();
        try (InputStream is = c.getInputStream()) { while (is.read() != -1) {} }

        // allow some time for async rebuild to finish
        Thread.sleep(1000);

        assertTrue(Files.exists(snapshot), "Snapshot file should exist after rebuild");
    }
}
