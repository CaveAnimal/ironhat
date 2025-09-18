package com.codetalker.embedding;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified ModelLoader that supports a lightweight mock mode and a simple
 * file-presence-based loadModel API used by integration tests.
 */
public class ModelLoader {
    private final boolean mockMode;
    private boolean loaded = false;
    private Path configuredPath = null;
    private Object interpreterInstance = null; // org.tensorflow.lite.Interpreter if available
    private Object savedModelBundleInstance = null; // org.tensorflow.SavedModelBundle when available
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public ModelLoader() {
        this.mockMode = Boolean.getBoolean("codetalker.model.mock");
    }

    public ModelLoader(String path) {
        this.mockMode = Boolean.getBoolean("codetalker.model.mock");
        this.configuredPath = path == null ? null : Path.of(path);
    }

    public boolean isMock() { return mockMode; }

    /**
     * Backwards-compatible API used by existing tests.
     * Returns true if file exists (or mock mode enabled), sets internal loaded flag.
     */
    public boolean loadModel(String path) {
        if (mockMode) { loaded = true; return true; }
        File f = new File(path);
        if (!f.exists()) { loaded = false; return false; }
        // If TensorFlow/TFLite is available on the classpath, try to initialize it reflectively.
        try {
            // Try TFLite Interpreter first. Be permissive about constructor signatures.
            Class<?> tfliteCls = Class.forName("org.tensorflow.lite.Interpreter");
            Constructor<?> chosen = null;
            Object ctorArg = null;
            for (Constructor<?> c : tfliteCls.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1) {
                    Class<?> p = params[0];
                    try {
                        if (p.equals(java.io.File.class)) {
                            chosen = c; ctorArg = f; break;
                        } else if (p.equals(java.nio.MappedByteBuffer.class) || p.equals(java.nio.ByteBuffer.class)) {
                            // attempt to create a ByteBuffer from file contents
                            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
                            ctorArg = bb;
                            chosen = c;
                            break;
                        } else if (p.equals(byte[].class)) {
                            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                            ctorArg = bytes; chosen = c; break;
                        }
                    } catch (Throwable t) {
                        // ignore and try other constructors
                    }
                }
            }
            if (chosen != null) {
                interpreterInstance = chosen.newInstance(ctorArg);
                loaded = true;
                logger.info("Initialized TensorFlow Lite Interpreter reflectively (flexible)");
                return true;
            }
        } catch (ClassNotFoundException cnf) {
            // TFLite not present - that's fine
        } catch (Exception e) {
            logger.warn("TFLite interpreter init failed (ignored)", e);
        }

        try {
            // Try SavedModelBundle (full TF Java) with several common signatures
            Class<?> smbCls = Class.forName("org.tensorflow.SavedModelBundle");
            try {
                Method loadMethod = smbCls.getMethod("load", String.class);
                savedModelBundleInstance = loadMethod.invoke(null, path);
                loaded = true;
                logger.info("Loaded SavedModelBundle reflectively (String)");
                return true;
            } catch (NoSuchMethodException ns1) {
                try {
                    Method loadMethod2 = smbCls.getMethod("load", java.nio.file.Path.class);
                    savedModelBundleInstance = loadMethod2.invoke(null, java.nio.file.Path.of(path));
                    loaded = true;
                    logger.info("Loaded SavedModelBundle reflectively (Path)");
                    return true;
                } catch (NoSuchMethodException ns2) {
                    // fall through
                }
            }
        } catch (ClassNotFoundException cnf) {
            // full TF not present - ok
        } catch (Exception e) {
            logger.warn("SavedModelBundle init failed (ignored)", e);
        }

        // If no TF runtime available, treat file-presence as success
        loaded = true;
        return true;
    }

    public boolean loadModel() {
        if (configuredPath == null) return false;
        return loadModel(configuredPath.toString());
    }

    public boolean isModelLoaded() { return loaded; }

    public Object getInterpreterInstance() { return interpreterInstance; }
    public Object getSavedModelBundleInstance() { return savedModelBundleInstance; }

    /**
     * Test-only helper: inject a SavedModelBundle (or test stub) instance and mark the loader loaded.
     * Package-private to avoid exposing in production API surface.
     */
    void setSavedModelBundleInstance(Object smb) {
        this.savedModelBundleInstance = smb;
        this.loaded = true;
    }

    /**
     * Returns true if either a TFLite Interpreter or SavedModelBundle was created
     * during `loadModel` reflective initialization.
     */
    public boolean isTensorflowRuntimeAvailable() {
        return interpreterInstance != null || savedModelBundleInstance != null;
    }

    public void close() { loaded = false; /* no-op for now */ }

    // New, simpler API for programmatic load (no native TF here)
    public void load() {
        if (mockMode) { loaded = true; return; }
        if (configuredPath == null) throw new IllegalStateException("No model path provided and not in mock mode");
        if (!Files.exists(configuredPath)) throw new IllegalStateException("Model file not found: " + configuredPath);
        // attempt to initialize via reflective TF if available
        loadModel(configuredPath.toString());
    }
}
