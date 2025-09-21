package com.codetalker.embedding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class FloatSerializationUtils {
    private FloatSerializationUtils() {}

    public static byte[] floatsToBytes(float[] values) {
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : values) bb.putFloat(f);
        return bb.array();
    }

    public static float[] bytesToFloats(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int n = bytes.length / 4;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = bb.getFloat(i * 4);
        return out;
    }
}
