package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import ai.mazehunt.memory.testsupport.FakeEmbedder;
import ai.mazehunt.memory.testsupport.FakeModelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LayeredMemoryServiceTest {

    private static final List<String> VOCAB = List.of(
            "vram", "workstation", "markdown", "color", "orange",
            "tea", "coffee", "stock", "buffett");

    private SqliteMemoryStore store;
    private InMemoryVectorIndex index;

    @BeforeEach
    void setup(@TempDir Path tmp) {
        store = new SqliteMemoryStore(tmp.resolve("mem.db"));
        index = new InMemoryVectorIndex();
    }

    @AfterEach
    void tearDown() { if (store != null) store.close(); }

    // ---------------- basics ----------------

    @Test
    void remembersAndRecallsWithFtsOnly() {
        MemoryService svc = svc(null, null, 100, 0.0);
        svc.remember("User owns a 48 GB VRAM workstation", Map.of(), "test");
        List<MemoryItem> hits = svc.recall(RecallRequest.of("VRAM workstation", 400));
        assertFalse(hits.isEmpty());
        assertTrue(hits.get(0).content().contains("VRAM"));
    }

    @Test
    void statsReflectInsertsPerTier() {
        MemoryService svc = svc(null, null, 100, 0.0);
        svc.remember("ep1", Map.of(), "t");
        svc.remember("ep2", Map.of(), "t");
        svc.learnFact("a fact", "setup", 0.95);
        assertEquals(2L, svc.stats().get(MemoryItem.Tier.EPISODIC));
        assertEquals(1L, svc.stats().get(MemoryItem.Tier.SEMANTIC));
    }

    // ---------------- recall filters ----------------

    @Test
    void recallRespectsConfidenceFloor() {
        MemoryService svc = svc(null, null, 100, 0.0);
        svc.learnFact("high-confidence fact about tea", "src", 0.95);
        svc.learnFact("low-confidence fact about tea", "src", 0.20);

        List<MemoryItem> hits = svc.recall(new RecallRequest(
                "tea", 500, null, Map.of(), 0.5));
        assertEquals(1, hits.size());
        assertTrue(hits.get(0).content().contains("high-confidence"));
    }

    @Test
    void recallRespectsTierFilter() {
        MemoryService svc = svc(null, null, 100, 0.0);
        svc.remember("episodic tea note", Map.of(), "src");
        svc.learnFact("semantic tea fact", "src", 0.9);

        List<MemoryItem> semanticOnly = svc.recall(new RecallRequest(
                "tea", 400, Set.of(MemoryItem.Tier.SEMANTIC), Map.of(), 0.0));
        assertEquals(1, semanticOnly.size());
        assertEquals(MemoryItem.Tier.SEMANTIC, semanticOnly.get(0).tier());
    }

    @Test
    void recallRespectsRequiredTags() {
        MemoryService svc = svc(null, null, 100, 0.0);
        svc.remember("tea note A", Map.of("session", "s1"), "src");
        svc.remember("tea note B", Map.of("session", "s2"), "src");

        List<MemoryItem> onlyS1 = svc.recall(new RecallRequest(
                "tea", 400, null, Map.of("session", "s1"), 0.0));
        assertEquals(1, onlyS1.size());
        assertEquals("s1", onlyS1.get(0).tags().get("session"));
    }

    @Test
    void recallRespectsTokenBudget() {
        MemoryService svc = svc(null, null, 100, 0.0);
        for (int i = 0; i < 10; i++) {
            // Large-ish content so the budget bites before we run out of items.
            svc.remember("tea paragraph number " + i + " " + "lorem ".repeat(40),
                    Map.of(), "src");
        }
        List<MemoryItem> hits = svc.recall(RecallRequest.of("tea", 120));
        int totalTokens = hits.stream().mapToInt(MemoryItem::tokens).sum();
        assertTrue(totalTokens <= 120,
                "retrieval blew past budget: " + totalTokens);
        assertFalse(hits.isEmpty(), "should still return at least one item");
    }

    // ---------------- vector similarity + MMR ----------------

    @Test
    void recallWithEmbedderPrefersSemanticMatches() throws InterruptedException {
        FakeEmbedder embedder = new FakeEmbedder(VOCAB);
        MemoryService svc = svc(embedder, null, 100, 0.0);
        svc.learnFact("User owns a high-end workstation",             "src", 0.9);
        svc.learnFact("User's favourite colour is orange",            "src", 0.9);
        svc.learnFact("Stock analysis in the style of Buffett",       "src", 0.9);
        waitForAsyncEmbeds();

        List<MemoryItem> hits = svc.recall(RecallRequest.of("my workstation setup", 400));
        assertFalse(hits.isEmpty());
        // The workstation fact must rank ahead of the colour / stock ones.
        assertTrue(hits.get(0).content().toLowerCase().contains("workstation"));
    }

    @Test
    void mmrDropsNearDuplicates() throws InterruptedException {
        FakeEmbedder embedder = new FakeEmbedder(VOCAB);
        MemoryService svc = svc(embedder, null, 100, 0.0);
        // Three facts that all embed to roughly the same vector.
        svc.learnFact("User has workstation",                  "s1", 0.9);
        svc.learnFact("User has workstation too",              "s2", 0.9);
        svc.learnFact("User has workstation really",           "s3", 0.9);
        waitForAsyncEmbeds();

        List<MemoryItem> hits = svc.recall(RecallRequest.of("workstation", 400));
        // MMR should suppress at least one of the three near-duplicates.
        assertTrue(hits.size() < 3, "expected MMR to drop duplicates, got " + hits.size());
    }

    // ---------------- consolidation ----------------

    @Test
    void consolidateProducesSemanticFactsFromRecentEpisodes() {
        // Summariser emits two facts per call, one per line.
        FakeModelClient summariser = new FakeModelClient(prompt ->
                "User prefers Markdown answers\nUser drinks oolong tea");
        MemoryService svc = svc(null, summariser, 100, 0.0);
        for (int i = 0; i < 12; i++) svc.remember("episode " + i, Map.of(), "chat");
        long semanticBefore = svc.stats().get(MemoryItem.Tier.SEMANTIC);

        svc.consolidate();

        long semanticAfter = svc.stats().get(MemoryItem.Tier.SEMANTIC);
        assertEquals(semanticBefore + 2, semanticAfter);
        assertEquals(1, summariser.invocations.get());
    }

    @Test
    void consolidateNoopWhenTooFewEpisodes() {
        FakeModelClient summariser = new FakeModelClient("never called");
        MemoryService svc = svc(null, summariser, 100, 0.0);
        for (int i = 0; i < 3; i++) svc.remember("episode " + i, Map.of(), "chat");

        svc.consolidate();

        assertEquals(0, summariser.invocations.get(),
                "summariser should not be called when fewer than 8 episodes exist");
    }

    @Test
    void consolidateSkipsMalformedOutput() {
        FakeModelClient summariser = new FakeModelClient("\n\n   \n");
        MemoryService svc = svc(null, summariser, 100, 0.0);
        for (int i = 0; i < 10; i++) svc.remember("episode " + i, Map.of(), "chat");

        svc.consolidate();

        assertEquals(0L, svc.stats().get(MemoryItem.Tier.SEMANTIC),
                "blank output must not pollute semantic memory with empty facts");
    }

    @Test
    void consolidateTolerateModelFailure() {
        FakeModelClient summariser = new FakeModelClient(p -> {
            throw new RuntimeException("kaboom");
        });
        MemoryService svc = svc(null, summariser, 100, 0.0);
        for (int i = 0; i < 10; i++) svc.remember("episode " + i, Map.of(), "chat");

        assertDoesNotThrow(svc::consolidate);
    }

    // ---------------- learnFact + id ----------------

    @Test
    void learnFactReturnsAStableId() {
        MemoryService svc = svc(null, null, 100, 0.0);
        String id = svc.learnFact("something", "src", 0.9);
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void rememberReturnsAStableId() {
        MemoryService svc = svc(null, null, 100, 0.0);
        String id = svc.remember("x", Map.of(), "src");
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    // ---------------- helpers ----------------

    private MemoryService svc(FakeEmbedder embedder, FakeModelClient summariser,
                              int consolidateEveryN, double confidenceFloor) {
        return new LayeredMemoryService(store, index, embedder, summariser,
                consolidateEveryN, confidenceFloor);
    }

    /** Embedding happens on a background thread; give it a beat. */
    private static void waitForAsyncEmbeds() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(200);
    }
}
