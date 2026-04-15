package ai.mazehunt.core.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.RecallRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LayeredMemoryServiceTest {

    @Test
    void remembersAndRecallsWithoutEmbedder(@TempDir Path tmp) {
        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("mem.db"))) {
            InMemoryVectorIndex index = new InMemoryVectorIndex();
            LayeredMemoryService svc = new LayeredMemoryService(store, index, null, null, 100, 0.0);

            svc.remember("The user prefers concise answers in Markdown.",
                    Map.of("kind", "preference"), "test");
            svc.remember("The user's favourite colour is orange.",
                    Map.of("kind", "profile"), "test");
            svc.learnFact("User owns a workstation with 48 GB VRAM.", "setup", 0.95);

            List<MemoryItem> hits = svc.recall(RecallRequest.of("workstation VRAM", 500));
            assertFalse(hits.isEmpty(), "should recall the VRAM fact via FTS");
            assertTrue(hits.stream().anyMatch(m -> m.content().contains("48 GB VRAM")));

            Map<MemoryItem.Tier, Long> stats = svc.stats();
            assertEquals(2L, stats.get(MemoryItem.Tier.EPISODIC));
            assertEquals(1L, stats.get(MemoryItem.Tier.SEMANTIC));
        }
    }
}
