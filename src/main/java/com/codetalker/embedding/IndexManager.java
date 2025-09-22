package com.codetalker.embedding;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import com.codetalker.ann.JelmerkAnnIndex;
import com.codetalker.storage.DatabaseManager;
import com.codetalker.util.ProcessLock;

/**
 * IndexManager - CLI and small HTTP endpoint to rebuild and persist the ANN index.
 */
public class IndexManager {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: IndexManager --db <dbPath> --out <snapshotPath> [--http <port>]");
            System.out.println("Optional flags: --dim <n> --m <n> --ef <n> --batch-size <n> --fetch-size <n> --lock-file <path>");
            System.out.println("  --batch-size: number of rows to fetch per paged query (default 1000)");
            System.out.println("  --fetch-size: JDBC fetch size hint (0=default, -1=driver streaming)");
            System.out.println("  --lock-file: file path used for cross-process rebuild locking (best-effort)");
            return;
        }
        String dbPath = null;
        String outPath = null;
        int httpPort = -1;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db": dbPath = args[++i]; break;
                case "--out": outPath = args[++i]; break;
                case "--http": httpPort = Integer.parseInt(args[++i]); break;
                case "--rebuild": /* noop */ break;
            }
        }
        if (dbPath == null || outPath == null) {
            System.err.println("--db and --out are required");
            return;
        }

        DatabaseManager db = new DatabaseManager(dbPath);

        // parse optional flags --dim, --m, --ef, --batch-size, --fetch-size, --lock-file
        Integer dimArg = null;
        int mArg = 16;
        int efArg = 10;
        int batchSize = 1000;
        int fetchSize = 0;
        String lockFile = null;
        for (int i = 0; i < args.length; i++) {
            if ("--dim".equals(args[i])) dimArg = Integer.parseInt(args[++i]);
            if ("--m".equals(args[i])) mArg = Integer.parseInt(args[++i]);
            if ("--ef".equals(args[i])) efArg = Integer.parseInt(args[++i]);
            if ("--batch-size".equals(args[i])) batchSize = Integer.parseInt(args[++i]);
            if ("--fetch-size".equals(args[i])) fetchSize = Integer.parseInt(args[++i]);
            if ("--lock-file".equals(args[i])) lockFile = args[++i];
        }

        EmbeddingBinaryRepository repo = new EmbeddingBinaryRepository(db);
        int detectedDim = dimArg != null ? dimArg : repo.detectVectorDimension();
        if (detectedDim <= 0) detectedDim = 128;
        final int dim = detectedDim;
        final JelmerkAnnIndex index = new JelmerkAnnIndex(dim, mArg, efArg, 2000);
        final Path outPathObj = Path.of(outPath);

        // compute max id in DB to avoid scanning a huge id space
        long maxId = 0;
        try (java.sql.Connection c = db.getConnection(); java.sql.PreparedStatement ps = c.prepareStatement("SELECT MAX(id) FROM embeddings")) {
            try (java.sql.ResultSet rs = ps.executeQuery()) { if (rs.next()) maxId = rs.getLong(1); }
        } catch (Exception ex) { /* ignore, fallback to scanning */ }

        final long finalMaxId = maxId <= 0 ? 1000000L : maxId;

    final java.util.concurrent.atomic.AtomicBoolean rebuildLock = new java.util.concurrent.atomic.AtomicBoolean(false);

    final int finalBatchSize = batchSize;
    final int finalFetchSize = fetchSize;
    final String finalLockFile = lockFile;

    Runnable rebuild = () -> {
            if (!rebuildLock.compareAndSet(false, true)) {
                System.out.println("Rebuild already in progress, skipping trigger");
                return;
            }
            try {
                System.out.println("Rebuilding index from DB (maxId=" + finalMaxId + ")...");
                // Acquire an inter-process lock if requested
                ProcessLock pl = null;
                if (finalLockFile != null) {
                    try {
                        java.sql.Connection maybePg = null;
                        try { maybePg = db.getConnection(); } catch (Exception ignore) { maybePg = null; }
                        pl = ProcessLock.tryAcquire(Paths.get(finalLockFile), maybePg);
                        if (pl == null) {
                            System.out.println("Another process holds the rebuild lock, skipping this rebuild");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to acquire process lock, proceeding with local lock only: " + e.getMessage());
                    }
                }
                PersistedVectorIndexAdapter adapter = new PersistedVectorIndexAdapter(repo, index, finalBatchSize, finalFetchSize);
                int added = adapter.preload();
                System.out.println("Added " + added + " vectors to index");
                Files.createDirectories(outPathObj.getParent());
                index.persistTo(outPathObj);
                System.out.println("Saved index snapshot to: " + outPathObj.toAbsolutePath());
                if (pl != null) {
                    try { pl.close(); } catch (Exception ignore) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rebuildLock.set(false);
            }
        };

        // Run initial rebuild
        rebuild.run();

        if (httpPort > 0) {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(httpPort), 0);
            server.createContext("/rebuild", exch -> {
                Executors.newSingleThreadExecutor().submit(() -> rebuild.run());
                String resp = "Rebuild triggered";
                exch.sendResponseHeaders(200, resp.length());
                try (java.io.OutputStream os = exch.getResponseBody()) {
                    os.write(resp.getBytes());
                }
            });
            server.setExecutor(Executors.newCachedThreadPool());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            server.start();
            System.out.println("HTTP rebuild endpoint available at http://localhost:" + httpPort + "/rebuild");
        }
    }
}
