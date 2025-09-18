package com.codetalker.ann;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

public class JelmerkAnnIndexPersistenceTest {

    @Test
    public void persistAndLoadRoundTrip() throws Exception {
        JelmerkAnnIndex idx = new JelmerkAnnIndex(3, 8, 10, 100);
        idx.add("a", new float[] {1f,0f,0f});
        idx.add("b", new float[] {0f,1f,0f});

        Path tmp = Files.createTempFile("jelmerk-test", ".idx");
        idx.persistTo(tmp);

        // create new instance and load
        JelmerkAnnIndex loaded = new JelmerkAnnIndex(3, 8, 10, 100);
        loaded.loadFrom(tmp);

    List<String> res = java.util.Arrays.asList(loaded.query(new float[] {1f,0f,0f}, 1));
    assertFalse(res.isEmpty());
    assertEquals("a", res.get(0));

        // cleanup
        Files.deleteIfExists(tmp);
    }
}
