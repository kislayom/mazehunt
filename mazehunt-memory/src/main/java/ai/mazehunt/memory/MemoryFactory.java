package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.memory.client.MemoryHttpClient;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;

import java.nio.file.Path;
import java.time.Duration;

/**
 * One-stop construction for the memory tier. Call {@link #local} to embed the
 * memory inside the same JVM as the agent, or {@link #remote} to point at a
 * standalone memory service.
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
}
