package com.codetalker.embedding;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ModelLoaderIntegrationTest {

    @Test
    void testLoadModelFromResources() {
        ModelLoader loader = new ModelLoader();
        // Models in tests are copied from src/main/resources to target/classes, so reference the resource path
        boolean result = loader.loadModel("src/main/resources/models/universal-sentence-encoder-lite.tflite");
        assertTrue(result, "ModelLoader should return true when model file is present");
        assertTrue(loader.isModelLoaded(), "ModelLoader should report model as loaded");
        loader.close();
    }
}
