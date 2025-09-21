package com.codetalker.embedding;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ModelLoaderTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("codetalker.model.mock");
        System.clearProperty("code.talker.model.path");
    }

    @Test
    public void testModelFilePresenceAndLoad() throws Exception {
        Path repoModel = Path.of("src", "main", "resources", "models", "universal-sentence-encoder-lite.tflite");
        assertTrue(Files.exists(repoModel), "Expected model file to be present in resources/models");

        ModelLoader loader = new ModelLoader(repoModel);
        // Should not throw and should mark model as loaded (file-presence fallback)
        loader.load();
        assertTrue(loader.isModelLoaded(), "ModelLoader should report model loaded after load()");
        // Instances for TF runtimes may be null in environments without TF/JNI
        assertNotNull(loader, "Loader instance should not be null");
    }

    @Test
    void loadInMockModeDoesNotThrow() {
        System.setProperty("codetalker.model.mock", "true");
        ModelLoader l = new ModelLoader((String) null);
        assertTrue(l.isMock());
        assertDoesNotThrow(l::load);
        assertTrue(l.isModelLoaded());
    }

    @Test
    void loadMissingFileThrows() {
        System.setProperty("codetalker.model.mock", "false");
        ModelLoader l = new ModelLoader("nonexistent-model-file.bin");
        Exception ex = assertThrows(IllegalStateException.class, l::load);
        assertTrue(ex.getMessage().contains("Model file not found"));
    }

    @Test
    void testLoadModelFileNotFound() {
        ModelLoader loader = new ModelLoader();
        boolean result = loader.loadModel("nonexistent/model.tflite");
        assertFalse(result);
        assertFalse(loader.isModelLoaded());
    }
}
