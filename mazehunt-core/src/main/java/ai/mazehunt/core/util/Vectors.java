package ai.mazehunt.core.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Dense vector math — keep it allocation-free on the hot path. */
public final class Vectors {
    private Vectors() {}

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static byte[] toBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }

    /** Decode a JSON numeric array into a dense float vector. */
    public static float[] fromJsonArray(JsonNode arr) {
        if (arr == null || !arr.isArray()) return new float[0];
        float[] out = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) out[i] = (float) arr.get(i).asDouble();
        return out;
    }

    public static float[] fromBytes(byte[] bytes) {
        if (bytes == null) return new float[0];
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < out.length; i++) out[i] = buf.getFloat();
        return out;
    }
}
