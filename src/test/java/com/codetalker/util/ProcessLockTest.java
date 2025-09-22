package com.codetalker.util;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * Tests for ProcessLock file-based locking behavior.
 */
public class ProcessLockTest {

    @Test
    public void testFileLockAcquisitionAndRelease() throws Exception {
        Path tmp = Files.createTempDirectory("processlock-test");
        Path lockFile = tmp.resolve("rebuild.lock");

        // Acquire the lock in one "process"
        ProcessLock p1 = ProcessLock.tryAcquire(lockFile, null);
        assertNotNull(p1, "First lock acquisition should succeed");

        // Second acquisition should fail (file lock held)
        ProcessLock p2 = ProcessLock.tryAcquire(lockFile, null);
        assertNull(p2, "Second lock acquisition should fail while first holds the lock");

        // Release first lock
        p1.close();

        // Now acquisition should succeed again
        ProcessLock p3 = ProcessLock.tryAcquire(lockFile, null);
        assertNotNull(p3, "Lock acquisition should succeed after release");
        if (p3 != null) p3.close();
    }
}
