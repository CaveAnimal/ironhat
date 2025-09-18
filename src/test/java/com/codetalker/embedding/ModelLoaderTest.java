package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ModelLoaderTest {

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
