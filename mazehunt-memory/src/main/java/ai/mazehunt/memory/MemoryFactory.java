package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.memory.client.MemoryHttpClient;
import ai.mazehunt.memory.compact.CompactionService;
import ai.mazehunt.memory.embed.OnnxEmbedder;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import ai.mazehunt.memory.verify.FactVerifier;

import java.nio.file.Path;
import java.time.Duration;

/**
 * One-stop construction for the memory tier. Picks between in-process
 * ({@link #local}) and remote ({@link #remote}), and optionally wires:
 *
 * <ul>
 *   <li>An in-process ONNX embedder (zero Ollama dependency for embeddings).</li>
 *   <li>A {@link CompactionService} for mid-session context compaction.</li>
 *   <li>A {@link FactVerifier} for self-healing memory recall.</li>
 * </ul>
 */
public final class MemoryFactory {

    private MemoryFactory() {}

    /** In-process layered memory. Owns the SQLite file and vector index. */
    public static MemoryService local(Path sqlitePath,
                                      EmbeddingClient embedder,
                                      ModelClient summariser,
                                      int consolidateEveryN,
                                      double confidenceFloor) {
        SqliteMemoryStore store = new SqliteMemoryStore(sqlitePath);
        InMemoryVectorIndex index = new InMemoryVectorIndex();
        return new LayeredMemoryService(store, index, embedder, summariser,
                consolidateEveryN, confidenceFloor);
    }

    /**
     * HTTP client that talks to a remote {@code mazehunt-memory-service}.
     * The agent process never touches SQLite — all state lives in the service.
     */
    public static MemoryService remote(String baseUrl) {
        return new MemoryHttpClient(baseUrl, Duration.ofSeconds(30));
    }

    public static MemoryService remote(String baseUrl, Duration timeout) {
        return new MemoryHttpClient(baseUrl, timeout);
    }

    /**
     * Try to create an in-process ONNX embedder from a model directory.
     * Returns {@code null} if ONNX Runtime isn't on the classpath or
     * the model files aren't found — caller should fall back to Ollama.
     */
    public static EmbeddingClient onnxEmbedder(Path modelDir, int maxSeqLen) {
        return OnnxEmbedder.tryCreate(modelDir, maxSeqLen);
    }

    /** Compaction service for mid-session context compression. */
    public static CompactionService compaction(ModelClient model, MemoryService memory) {
        if (model == null) return null;
        return new CompactionService(model, memory);
    }

    /**
     * Self-healing fact verifier. Verifies top-k recalled facts against
     * live context before they enter the prompt.
     */
    public static FactVerifier verifier(ModelClient model, int maxFacts,
                                        double outdatedPenalty, double unverifiablePenalty) {
        if (model == null) return FactVerifier.passThrough();
        return new FactVerifier(model, maxFacts, outdatedPenalty, unverifiablePenalty);
    }
}
