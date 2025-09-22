package com.codetalker.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * ProcessLock - best-effort multi-process lock using a file lock and optional Postgres advisory lock.
 */
public class ProcessLock implements AutoCloseable {
    private final FileOutputStream fos;
    private final FileLock lock;
    private final Connection pgConn;

    private ProcessLock(FileOutputStream fos, FileLock lock, Connection pgConn) {
        this.fos = fos;
        this.lock = lock;
        this.pgConn = pgConn;
    }

    public static ProcessLock tryAcquire(Path lockFile, Connection maybePgConn) {
        try {
            File f = lockFile.toFile();
            f.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(f, true);
            FileLock fl = fos.getChannel().tryLock();
            if (fl == null) {
                fos.close();
                return null;
            }
            // Try Postgres advisory lock if a Postgres connection is provided
            Connection pg = null;
            if (maybePgConn != null) {
                try {
                    PreparedStatement ps = maybePgConn.prepareStatement("SELECT pg_try_advisory_lock(?::bigint)");
                    ps.setLong(1, Math.abs(lockFile.toString().hashCode()));
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boolean got = rs.getBoolean(1);
                            if (got) {
                                pg = maybePgConn;
                            } else {
                                // couldn't get advisory lock; release file lock and return null
                                fl.release();
                                fos.close();
                                return null;
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // ignore advisory lock failures, proceed with file lock only
                }
            }
            return new ProcessLock(fos, fl, pg);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void close() {
        try {
            if (pgConn != null) {
                try {
                    var ps = pgConn.prepareStatement("SELECT pg_advisory_unlock(?::bigint)");
                    ps.setLong(1, Math.abs(new java.io.File(((FileOutputStream) fos).getFD().toString()).getPath().hashCode()));
                    ps.execute();
                } catch (Exception ignore) {}
            }
        } finally {
            try { if (lock != null && lock.isValid()) lock.release(); } catch (Exception ignore) {}
            try { if (fos != null) fos.close(); } catch (Exception ignore) {}
        }
    }
}
