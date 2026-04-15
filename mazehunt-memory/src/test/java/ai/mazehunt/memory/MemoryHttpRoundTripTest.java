package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.memory.client.MemoryHttpClient;
import ai.mazehunt.memory.service.MemoryHttpServer;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryHttpRoundTripTest {

    private MemoryHttpServer server;
    private MemoryHttpClient client;
    private SqliteMemoryStore store;

    @BeforeEach
    void start(@TempDir Path tmp) throws Exception {
        store = new SqliteMemoryStore(tmp.resolve("mem.db"));
        MemoryService backing = new LayeredMemoryService(
                store, new InMemoryVectorIndex(), null, null, 100, 0.0);
        // port=0 → let the OS pick a free port.
        server = new MemoryHttpServer(backing, 0, 4);
        server.start();
        client = new MemoryHttpClient("http://localhost:" + server.port(), Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop();
        if (store != null) store.close();
    }

    @Test
    void remembersAndRecallsOverHttp() {
        String id1 = client.remember("The user owns a 48 GB VRAM workstation.",
                Map.of("kind", "profile"), "unit-test");
        String id2 = client.learnFact("User prefers concise Markdown answers.",
                "unit-test", 0.9);
        assertNotNull(id1);
        assertNotNull(id2);

        List<MemoryItem> hits = client.recall(RecallRequest.of("workstation VRAM", 400));
        assertFalse(hits.isEmpty(), "remote recall should surface the VRAM fact");
        assertTrue(hits.stream().anyMatch(m -> m.content().contains("48 GB VRAM")));
        // Embeddings are stripped in transit.
        assertTrue(hits.stream().allMatch(m -> m.embedding() == null));

        Map<MemoryItem.Tier, Long> stats = client.stats();
        assertEquals(1L, stats.get(MemoryItem.Tier.EPISODIC));
        assertEquals(1L, stats.get(MemoryItem.Tier.SEMANTIC));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(MemoryHttpClient.MemoryServiceException.class,
                () -> client.remember("", Map.of(), "x"));
    }

    @Test
    void consolidateEndpointOk() {
        // Should not throw even with insufficient history.
        client.consolidate();
    }
}
