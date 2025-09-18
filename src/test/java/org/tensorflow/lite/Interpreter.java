package org.tensorflow.lite;

/**
 * Test stub for TFLite Interpreter used by unit tests only.
 */
public class Interpreter {
    private final float[] data;
    public Interpreter(java.io.File f) {
        this.data = new float[0];
    }
    public Interpreter(byte[] bytes) { this.data = new float[0]; }
    public Interpreter(java.nio.ByteBuffer bb) { this.data = new float[0]; }

    // run(Object input, Object output)
    public void run(Object in, Object out) {
        // default no-op; tests may override behavior by subclassing the fake used in tests
    }
}
