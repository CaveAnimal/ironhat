package org.tensorflow;

import java.nio.file.Path;

/**
 * Test stub for TensorFlow SavedModelBundle used in unit tests only.
 */
public class SavedModelBundle {
    public static SavedModelBundle load(String s) {
        return new SavedModelBundle();
    }
    public static SavedModelBundle load(Path p) {
        return new SavedModelBundle();
    }
}
