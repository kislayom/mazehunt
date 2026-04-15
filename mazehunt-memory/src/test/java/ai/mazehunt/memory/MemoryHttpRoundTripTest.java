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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests across the HTTP boundary. Each test boots the
 * service on an ephemeral port so they can run in parallel without colliding.
 */
class MemoryHttpRoundTripTest {

    private MemoryHttpServer server;
    private MemoryHttpClient client;
    private SqliteMemoryStore store;

    @BeforeEach
    void start(@TempDir Path tmp) throws Exception {
        store = new SqliteMemoryStore(tmp.resolve("mem.db"));
        MemoryService backing = new LayeredMemoryService(
                store, new InMemoryVectorIndex(), null, null, 100, 0.0);
        server = new MemoryHttpServer(backing, 0, 4);
        server.start();
        client = new MemoryHttpClient(baseUrl(), Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop();
        if (store != null) store.close();
    }

    // ------------- happy path -------------

    @Test
    void remembersAndRecallsOverHttp() {
        String id1 = client.remember("The user owns a 48 GB VRAM workstation.",
                Map.of("kind", "profile"), "unit-test");
        String id2 = client.learnFact("User prefers concise Markdown answers.",
                "unit-test", 0.9);
        assertNotNull(id1);
        assertNotNull(id2);

        List<MemoryItem> hits = client.recall(RecallRequest.of("workstation VRAM", 400));
        assertFalse(hits.isEmpty());
        assertTrue(hits.stream().anyMatch(m -> m.content().contains("48 GB VRAM")));
        // Embeddings are stripped in transit.
        assertTrue(hits.stream().allMatch(m -> m.embedding() == null));

        Map<MemoryItem.Tier, Long> stats = client.stats();
        assertEquals(1L, stats.get(MemoryItem.Tier.EPISODIC));
        assertEquals(1L, stats.get(MemoryItem.Tier.SEMANTIC));
    }

    @Test
    void recallWithTierFilterFlowsThroughHttp() {
        client.remember("episodic tea note", Map.of(), "t");
        client.learnFact("semantic tea fact", "t", 0.9);

        List<MemoryItem> semanticOnly = client.recall(new RecallRequest(
                "tea", 400, Set.of(MemoryItem.Tier.SEMANTIC), Map.of(), 0.0));
        assertEquals(1, semanticOnly.size());
        assertEquals(MemoryItem.Tier.SEMANTIC, semanticOnly.get(0).tier());
    }

    @Test
    void consolidateEndpointReturnsOk() {
        assertDoesNotThrow(() -> client.consolidate());
    }

    // ------------- contract / error handling -------------

    @Test
    void rejectsEmptyRememberWithClientException() {
        assertThrows(MemoryHttpClient.MemoryServiceException.class,
                () -> client.remember("", Map.of(), "t"));
    }

    @Test
    void rejectsEmptyLearnFactWithClientException() {
        assertThrows(MemoryHttpClient.MemoryServiceException.class,
                () -> client.learnFact("", "t", 0.9));
    }

    @Test
    void wrongMethodReturns405() throws Exception {
        HttpResponse<String> resp = raw()
                .send(HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/memory/stats"))
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                      HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    void malformedJsonBodyYields500NotConnectionDrop() throws Exception {
        HttpResponse<String> resp = raw()
                .send(HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/memory/items"))
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{ not valid json "))
                        .build(),
                      HttpResponse.BodyHandlers.ofString());
        assertEquals(500, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""),
                "error payload should be structured JSON, got: " + resp.body());
    }

    @Test
    void healthzIsAlwaysOk() throws Exception {
        HttpResponse<String> resp = raw()
                .send(HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/healthz")).GET().build(),
                      HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"status\":\"ok\""));
    }

    @Test
    void statsContractMatchesExpectedShape() throws Exception {
        client.remember("x", Map.of(), "t");
        HttpResponse<String> resp = raw()
                .send(HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/memory/stats")).GET().build(),
                      HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("EPISODIC"));
        assertTrue(resp.body().contains("SEMANTIC"));
    }

    // ------------- helpers -------------

    private String baseUrl() { return "http://localhost:" + server.port(); }

    private static HttpClient raw() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }
}
