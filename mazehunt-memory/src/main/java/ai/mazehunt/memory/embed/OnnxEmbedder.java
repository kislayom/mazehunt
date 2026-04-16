package ai.mazehunt.memory.embed;

import ai.mazehunt.api.model.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-process text embedder backed by ONNX Runtime + a HuggingFace tokenizer.
 *
 * <p>Design rationale:
 * <ul>
 *   <li>Eliminates the Ollama round-trip for embeddings — every remember() and
 *       recall() is now local.</li>
 *   <li>Model and tokenizer files live on disk, referenced by path — not bundled
 *       in the JAR (~90 MB per model).</li>
 *   <li>Dependencies are {@code <optional>true</optional>} in the POM so the
 *       memory module compiles on hosts without native libs. If ONNX Runtime or
 *       the DJL tokenizer are missing at runtime, {@link #isAvailable()} returns
 *       false and the factory falls back to the Ollama adapter.</li>
 * </ul>
 *
 * <p>Supported models (drop any SentenceTransformer-compatible ONNX export):
 * <ul>
 *   <li>{@code all-MiniLM-L6-v2}  (384-dim, 91 MB)</li>
 *   <li>{@code bge-small-en-v1.5}  (384-dim, 130 MB)</li>
 *   <li>{@code nomic-embed-text-v1.5} (768-dim, 548 MB)</li>
 * </ul>
 *
 * <p>Files expected under {@code modelDir}:
 * <pre>
 *   model.onnx          — the ONNX-exported transformer
 *   tokenizer.json       — HuggingFace tokenizer definition
 * </pre>
 */
public final class OnnxEmbedder implements EmbeddingClient, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OnnxEmbedder.class);

    private final Object ortEnv;        // OrtEnvironment
    private final Object ortSession;    // OrtSession
    private final Object tokenizer;     // ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
    private final int maxSeqLen;
    private volatile int dims = -1;

    // Reflective handles — populated once at construction.
    private final Method createTensor;
    private final Method sessionRun;
    private final Method tensorGetFloatBuffer;
    private final Method tokenizerEncode;

    private OnnxEmbedder(Path modelDir, int maxSeqLen) throws Exception {
        this.maxSeqLen = maxSeqLen;

        // ---- Load tokenizer via DJL ----
        Class<?> hfTokClass = Class.forName("ai.djl.huggingface.tokenizers.HuggingFaceTokenizer");
        Path tokenizerJson = modelDir.resolve("tokenizer.json");
        this.tokenizer = hfTokClass.getMethod("newInstance", java.nio.file.Path.class)
                .invoke(null, tokenizerJson);
        this.tokenizerEncode = hfTokClass.getMethod("encode", String.class);

        // ---- Load ONNX model ----
        Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
        this.ortEnv = envClass.getMethod("getEnvironment").invoke(null);
        Class<?> sessClass = Class.forName("ai.onnxruntime.OrtSession");
        this.ortSession = envClass.getMethod("createSession", String.class)
                .invoke(ortEnv, modelDir.resolve("model.onnx").toString());

        Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        this.createTensor = tensorClass.getMethod("createTensor",
                Class.forName("ai.onnxruntime.OrtEnvironment"), LongBuffer.class, long[].class);
        this.sessionRun = sessClass.getMethod("run", Map.class);
        Class<?> valueClass = Class.forName("ai.onnxruntime.OnnxValue");
        this.tensorGetFloatBuffer = tensorClass.getMethod("getFloatBuffer");

        log.info("OnnxEmbedder loaded from {}", modelDir);
    }

    /**
     * Try to construct an ONNX embedder. Returns {@code null} if the native
     * libraries are missing or the model files don't exist.
     */
    public static OnnxEmbedder tryCreate(Path modelDir, int maxSeqLen) {
        if (!Files.isRegularFile(modelDir.resolve("model.onnx")) ||
            !Files.isRegularFile(modelDir.resolve("tokenizer.json"))) {
            log.info("ONNX model files not found at {} — using fallback embedder", modelDir);
            return null;
        }
        try {
            return new OnnxEmbedder(modelDir, maxSeqLen);
        } catch (ClassNotFoundException e) {
            log.info("ONNX Runtime / DJL tokenizer not on classpath — using fallback embedder");
            return null;
        } catch (Exception e) {
            log.warn("Failed to initialise OnnxEmbedder: {}", e.toString());
            return null;
        }
    }

    public static boolean isAvailable() {
        try {
            Class.forName("ai.onnxruntime.OrtEnvironment");
            Class.forName("ai.djl.huggingface.tokenizers.HuggingFaceTokenizer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public int dimensions() {
        if (dims < 0) embed("test");
        return dims;
    }

    @Override
    public float[] embed(String text) {
        try {
            // Tokenize.
            Object encoding = tokenizerEncode.invoke(tokenizer, text == null ? "" : text);
            long[] inputIds = (long[]) encoding.getClass().getMethod("getIds").invoke(encoding);
            long[] attentionMask = (long[]) encoding.getClass().getMethod("getAttentionMask").invoke(encoding);
            long[] tokenTypeIds = new long[inputIds.length]; // zeros

            // Truncate / pad to maxSeqLen.
            inputIds = padOrTruncate(inputIds, maxSeqLen);
            attentionMask = padOrTruncate(attentionMask, maxSeqLen);
            tokenTypeIds = padOrTruncate(tokenTypeIds, maxSeqLen);

            long[] shape = {1, maxSeqLen};
            Object idsTensor = createTensor.invoke(null, ortEnv,
                    LongBuffer.wrap(inputIds), shape);
            Object maskTensor = createTensor.invoke(null, ortEnv,
                    LongBuffer.wrap(attentionMask), shape);
            Object typeTensor = createTensor.invoke(null, ortEnv,
                    LongBuffer.wrap(tokenTypeIds), shape);

            // Run inference.
            Map<String, Object> inputs = Map.of(
                    "input_ids", idsTensor,
                    "attention_mask", maskTensor,
                    "token_type_ids", typeTensor);
            Object results = sessionRun.invoke(ortSession, inputs);

            // Extract first output → [1, seq_len, dims] or [1, dims].
            Object firstOutput = results.getClass().getMethod("get", int.class)
                    .invoke(results, 0);
            Object valuePart = firstOutput.getClass().getMethod("getValue").invoke(firstOutput);
            // Mean-pool the sequence dimension.
            float[] pooled = meanPool(valuePart, inputIds.length);
            dims = pooled.length;

            // Close tensors.
            for (Object t : List.of(idsTensor, maskTensor, typeTensor)) {
                t.getClass().getMethod("close").invoke(t);
            }
            results.getClass().getMethod("close").invoke(results);

            return normalise(pooled);
        } catch (Exception e) {
            throw new RuntimeException("ONNX embed failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) out.add(embed(t));
        return out;
    }

    @Override
    public void close() {
        try {
            ortSession.getClass().getMethod("close").invoke(ortSession);
        } catch (Exception ignored) {}
    }

    // ---------- helpers ----------

    private static long[] padOrTruncate(long[] arr, int len) {
        if (arr.length == len) return arr;
        long[] out = new long[len];
        System.arraycopy(arr, 0, out, 0, Math.min(arr.length, len));
        return out;
    }

    /** Mean-pool a [1, seq_len, hidden] tensor along the sequence axis. */
    private static float[] meanPool(Object tensor3d, int seqLen) throws Exception {
        // tensor3d is float[][][] for shape [1, seq_len, hidden].
        if (tensor3d.getClass().isArray() && tensor3d.getClass().getComponentType().isArray()) {
            Object batch0 = Array.get(tensor3d, 0); // [seq_len, hidden]
            if (batch0.getClass().getComponentType().isArray()) {
                float[] first = (float[]) Array.get(batch0, 0);
                int hidden = first.length;
                float[] sum = new float[hidden];
                int actualLen = Math.min(seqLen, Array.getLength(batch0));
                for (int s = 0; s < actualLen; s++) {
                    float[] row = (float[]) Array.get(batch0, s);
                    for (int h = 0; h < hidden; h++) sum[h] += row[h];
                }
                for (int h = 0; h < hidden; h++) sum[h] /= actualLen;
                return sum;
            }
            // [1, hidden] — already pooled.
            return (float[]) batch0;
        }
        // Flat float[] — assume already pooled.
        return (float[]) tensor3d;
    }

    private static float[] normalise(float[] v) {
        double norm = 0;
        for (float f : v) norm += f * f;
        if (norm < 1e-12) return v;
        norm = Math.sqrt(norm);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }
}
