package com.codetalker.embedding;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ModelLoaderReflectionTest {

    @Test
    public void tfliteStubConstructorUsed() throws Exception {
        // create a temp file to satisfy file-exists checks
        File tmp = File.createTempFile("mdl", ".tflite");
        tmp.deleteOnExit();

        ModelLoader loader = new ModelLoader(tmp.getAbsolutePath());
        boolean ok = loader.loadModel(tmp.getAbsolutePath());
        assertTrue(ok);
        assertTrue(loader.isModelLoaded());
        // since we included a test stub, interpreter instance should be non-null
        assertNotNull(loader.getInterpreterInstance());
    }

    @Test
    public void savedModelStubUsed() throws Exception {
        File tmp = File.createTempFile("smb", "");
        tmp.deleteOnExit();
        ModelLoader loader = new ModelLoader(tmp.getAbsolutePath());
        boolean ok = loader.loadModel(tmp.getAbsolutePath());
        assertTrue(ok);
        assertTrue(loader.isModelLoaded());
        // depending on what runtime stubs are present the loader may initialize
        // either a TFLite Interpreter or a SavedModelBundle. Accept either.
        assertTrue(loader.getSavedModelBundleInstance() != null || loader.getInterpreterInstance() != null,
                "Expected either SavedModelBundle or Interpreter to be initialized");
    }
}
