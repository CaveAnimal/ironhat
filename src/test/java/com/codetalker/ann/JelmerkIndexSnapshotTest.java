package com.codetalker.ann;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

public class JelmerkIndexSnapshotTest {

    @Test
    public void testPersistAndLoad() throws Exception {
        int dim = 3;
        JelmerkAnnIndex idx = new JelmerkAnnIndex(dim);
        idx.add("a", new float[] {1.0f, 0f, 0f});
        idx.add("b", new float[] {0f, 1.0f, 0f});
        idx.add("c", new float[] {0f, 0f, 1.0f});
        idx.build();

        Path tmp = Files.createTempFile("jelmerk-idx", ".snap");
        try {
            idx.persistTo(tmp);

            JelmerkAnnIndex loaded = new JelmerkAnnIndex(dim);
            loaded.loadFrom(tmp);

            String[] qA = loaded.query(new float[] {1.0f, 0f, 0f}, 1);
            assertArrayEquals(new String[] {"a"}, qA);
            String[] qB = loaded.query(new float[] {0f, 1.0f, 0f}, 1);
            assertArrayEquals(new String[] {"b"}, qB);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }
}
