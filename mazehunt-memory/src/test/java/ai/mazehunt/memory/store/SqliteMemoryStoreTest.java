package ai.mazehunt.memory.store;

import ai.mazehunt.api.memory.MemoryItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteMemoryStoreTest {

    // ---------- CRUD ----------

    @Test
    void upsertAndFindById(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            MemoryItem original = item("hello world", MemoryItem.Tier.EPISODIC, Map.of("k", "v"));
            store.upsert(original);

            Optional<MemoryItem> loaded = store.findById(original.id());
            assertTrue(loaded.isPresent());
            assertEquals("hello world", loaded.get().content());
            assertEquals(MemoryItem.Tier.EPISODIC, loaded.get().tier());
            assertEquals("v", loaded.get().tags().get("k"));
        }
    }

    @Test
    void upsertReplacesExistingItem(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            MemoryItem a = item("first", MemoryItem.Tier.EPISODIC, Map.of());
            store.upsert(a);
            MemoryItem b = new MemoryItem(a.id(), MemoryItem.Tier.SEMANTIC, "second",
                    null, Map.of(), "src2", 0.8,
                    a.createdAt(), Instant.now(), 0, 6);
            store.upsert(b);

            MemoryItem after = store.findById(a.id()).orElseThrow();
            assertEquals("second", after.content());
            assertEquals(MemoryItem.Tier.SEMANTIC, after.tier());
            // upsert bumps access_count on conflict.
            assertEquals(1, after.accessCount());
        }
    }

    @Test
    void deleteRemovesItem(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            MemoryItem a = item("gone", MemoryItem.Tier.EPISODIC, Map.of());
            store.upsert(a);
            store.delete(a.id());
            assertFalse(store.findById(a.id()).isPresent());
        }
    }

    // ---------- recent / counts ----------

    @Test
    void recentReturnsNewestFirstAndRespectsLimit(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            Instant base = Instant.now();
            for (int i = 0; i < 5; i++) {
                store.upsert(withCreated("e" + i, MemoryItem.Tier.EPISODIC,
                        base.plusSeconds(i)));
            }
            List<MemoryItem> recent = store.recent(MemoryItem.Tier.EPISODIC, 3);
            assertEquals(3, recent.size());
            assertEquals("e4", recent.get(0).content());
            assertEquals("e3", recent.get(1).content());
            assertEquals("e2", recent.get(2).content());
        }
    }

    @Test
    void countsByTierReturnsZeroMapWhenEmpty(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            Map<MemoryItem.Tier, Long> counts = store.countsByTier();
            for (MemoryItem.Tier t : MemoryItem.Tier.values()) {
                assertEquals(0L, counts.get(t), () -> "tier " + t + " should be empty");
            }
        }
    }

    @Test
    void countsByTierReflectsInserts(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            store.upsert(item("a", MemoryItem.Tier.EPISODIC, Map.of()));
            store.upsert(item("b", MemoryItem.Tier.EPISODIC, Map.of()));
            store.upsert(item("c", MemoryItem.Tier.SEMANTIC, Map.of()));
            var counts = store.countsByTier();
            assertEquals(2L, counts.get(MemoryItem.Tier.EPISODIC));
            assertEquals(1L, counts.get(MemoryItem.Tier.SEMANTIC));
            assertEquals(0L, counts.get(MemoryItem.Tier.PROCEDURAL));
        }
    }

    // ---------- FTS ----------

    @Test
    void ftsSearchMatchesSubstringOfContent(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            store.upsert(item("User owns a 48 GB VRAM workstation", MemoryItem.Tier.SEMANTIC, Map.of()));
            store.upsert(item("Weather is nice today", MemoryItem.Tier.EPISODIC, Map.of()));

            List<MemoryItem> hits = store.ftsSearch("VRAM workstation", 10);
            assertEquals(1, hits.size());
            assertTrue(hits.get(0).content().contains("VRAM"));
        }
    }

    @Test
    void ftsSearchHandlesSpecialCharsWithoutThrowing(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            store.upsert(item("price: $120 AAPL", MemoryItem.Tier.EPISODIC, Map.of()));
            // Any of these would blow up if we passed them straight to FTS5.
            assertDoesNotThrow(() -> store.ftsSearch("AAPL: $?", 5));
            assertDoesNotThrow(() -> store.ftsSearch("\"NOT\" AND price", 5));
            assertDoesNotThrow(() -> store.ftsSearch("(foo) OR [bar]", 5));
        }
    }

    @Test
    void ftsSearchEmptyOrShortTokensReturnsNothing(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            store.upsert(item("anything", MemoryItem.Tier.EPISODIC, Map.of()));
            assertTrue(store.ftsSearch("", 5).isEmpty());
            assertTrue(store.ftsSearch("a b", 5).isEmpty(), "single-char tokens are dropped");
        }
    }

    // ---------- touch ----------

    @Test
    void touchUpdatesLastAccessedAndCount(@TempDir Path tmp) throws InterruptedException {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            MemoryItem a = item("note", MemoryItem.Tier.EPISODIC, Map.of());
            store.upsert(a);
            Instant beforeTouch = store.findById(a.id()).orElseThrow().lastAccessedAt();

            Thread.sleep(10);
            store.touch(a.id());
            store.touch(a.id());

            MemoryItem after = store.findById(a.id()).orElseThrow();
            assertTrue(after.lastAccessedAt().isAfter(beforeTouch));
            assertEquals(2, after.accessCount());
        }
    }

    // ---------- persistence ----------

    @Test
    void dataSurvivesReopen(@TempDir Path tmp) {
        Path dbPath = tmp.resolve("m.db");
        String id;
        try (SqliteMemoryStore store = new SqliteMemoryStore(dbPath)) {
            MemoryItem a = item("persist me", MemoryItem.Tier.SEMANTIC, Map.of("k", "v"));
            store.upsert(a);
            id = a.id();
        }
        try (SqliteMemoryStore reopened = new SqliteMemoryStore(dbPath)) {
            MemoryItem loaded = reopened.findById(id).orElseThrow();
            assertEquals("persist me", loaded.content());
            assertEquals("v", loaded.tags().get("k"));
            assertEquals(1L, reopened.countsByTier().get(MemoryItem.Tier.SEMANTIC));
        }
    }

    @Test
    void embeddingRoundTripsAsBlob(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            float[] v = {1.5f, -2.0f, 3.25f, 0.0f};
            MemoryItem a = new MemoryItem(UUID.randomUUID().toString(),
                    MemoryItem.Tier.SEMANTIC, "with embedding", v,
                    Map.of(), "src", 0.9,
                    Instant.now(), Instant.now(), 0, 4);
            store.upsert(a);
            MemoryItem loaded = store.findById(a.id()).orElseThrow();
            assertArrayEquals(v, loaded.embedding(), 1e-6f);
        }
    }

    @Test
    void allReturnsEverything(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("m.db"))) {
            for (int i = 0; i < 7; i++) {
                store.upsert(item("item-" + i, MemoryItem.Tier.EPISODIC, Map.of()));
            }
            assertEquals(7, store.all().size());
        }
    }

    // ---------- helpers ----------

    private static MemoryItem item(String content, MemoryItem.Tier tier, Map<String, String> tags) {
        Instant now = Instant.now();
        return new MemoryItem(UUID.randomUUID().toString(), tier, content, null,
                tags, "test", 0.9, now, now, 0, content.length() / 4);
    }

    private static MemoryItem withCreated(String content, MemoryItem.Tier tier, Instant created) {
        return new MemoryItem(UUID.randomUUID().toString(), tier, content, null,
                Map.of(), "test", 0.9, created, created, 0, content.length() / 4);
    }
}
