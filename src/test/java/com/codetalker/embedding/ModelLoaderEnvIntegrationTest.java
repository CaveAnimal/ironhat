package com.codetalker.embedding;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ModelLoaderEnvIntegrationTest {

    @Test
    void testModelLoaderUsesProvidedPath() throws Exception {
        Path tmp = Files.createTempDirectory("codetalker-model-test");
        // create a dummy file to simulate model presence
        Path f = tmp.resolve("dummy-model.bin");
        Files.writeString(f, "dummy");

        // instantiate ModelLoader by passing explicit path
        ModelLoader loader = ModelLoader.fromPathOrEnv(tmp);
        loader.load();
        assertTrue(loader.isModelLoaded(), "ModelLoader should report loaded when given explicit path");

        // cleanup
        Files.deleteIfExists(f);
        Files.deleteIfExists(tmp);
    }
}
