package ai.mazehunt.memory.store;

import ai.mazehunt.api.memory.MemoryItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryVectorIndexTest {

    @Test
    void topKEmptyIndexReturnsEmpty() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        assertTrue(idx.topK(new float[]{1, 0, 0}, 5).isEmpty());
    }

    @Test
    void topKReturnsClosestOnHead() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        idx.add("exact",  new float[]{1, 0, 0});
        idx.add("close",  new float[]{0.9f, 0.1f, 0});
        idx.add("far",    new float[]{0, 0, 1});

        List<VectorIndex.Hit> hits = idx.topK(new float[]{1, 0, 0}, 3);
        assertEquals(3, hits.size());
        assertEquals("exact", hits.get(0).id());
        assertEquals("close", hits.get(1).id());
        assertEquals("far",   hits.get(2).id());
    }

    @Test
    void topKOrderedByScoreDescending() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        idx.add("a", new float[]{1, 0});
        idx.add("b", new float[]{0.5f, 0.5f});
        idx.add("c", new float[]{0, 1});

        List<VectorIndex.Hit> hits = idx.topK(new float[]{1, 0}, 3);
        for (int i = 1; i < hits.size(); i++) {
            assertTrue(hits.get(i - 1).score() >= hits.get(i).score(),
                    "hit " + i + " should not outrank its predecessor");
        }
    }

    @Test
    void topKRespectsLimit() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        for (int i = 0; i < 20; i++) {
            idx.add("v" + i, new float[]{i, 0});
        }
        assertEquals(5, idx.topK(new float[]{1, 0}, 5).size());
        assertEquals(1, idx.topK(new float[]{1, 0}, 1).size());
    }

    @Test
    void nullQueryReturnsEmpty() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        idx.add("a", new float[]{1, 0});
        assertTrue(idx.topK(null, 5).isEmpty());
    }

    @Test
    void addThenRemoveNoLongerReturnsId() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        idx.add("x", new float[]{1, 0});
        idx.add("y", new float[]{0, 1});
        assertEquals(2, idx.size());

        idx.remove("x");
        assertEquals(1, idx.size());
        List<VectorIndex.Hit> hits = idx.topK(new float[]{1, 0}, 5);
        assertEquals(1, hits.size());
        assertEquals("y", hits.get(0).id());
    }

    @Test
    void addIgnoresNullVector() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        idx.add("x", null);
        assertEquals(0, idx.size());
    }

    @Test
    void hydrateLoadsOnlyItemsWithEmbeddings() {
        InMemoryVectorIndex idx = new InMemoryVectorIndex();
        Instant now = Instant.now();
        MemoryItem withEmbedding = new MemoryItem("a", MemoryItem.Tier.SEMANTIC, "a", new float[]{1, 0},
                Map.of(), "t", 1.0, now, now, 0, 1);
        MemoryItem noEmbedding = new MemoryItem("b", MemoryItem.Tier.SEMANTIC, "b", null,
                Map.of(), "t", 1.0, now, now, 0, 1);
        idx.hydrate(List.of(withEmbedding, noEmbedding));
        assertEquals(1, idx.size());
        assertEquals("a", idx.topK(new float[]{1, 0}, 1).get(0).id());
    }
}
