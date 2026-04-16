package ai.mazehunt.memory.verify;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.memory.testsupport.FakeModelClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FactVerifierTest {

    @Test
    void confirmedFactPassesThroughUnchanged() {
        FakeModelClient model = new FakeModelClient("CONFIRMED");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("Build uses Maven", 0.9));
        List<MemoryItem> result = v.verify(items, "pom.xml exists, uses Maven", 0.3);

        assertEquals(1, result.size());
        assertEquals(0.9, result.get(0).confidence(), 1e-9);
    }

    @Test
    void outdatedFactGetsDemoted() {
        FakeModelClient model = new FakeModelClient("OUTDATED");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("Build uses Gradle", 0.9));
        // 0.9 * 0.2 = 0.18 — set floor below that so the item survives (demoted, not dropped)
        List<MemoryItem> result = v.verify(items, "pom.xml exists, uses Maven", 0.1);

        assertEquals(1, result.size());
        assertEquals(0.9 * 0.2, result.get(0).confidence(), 1e-9);
    }

    @Test
    void outdatedFactDroppedWhenBelowFloor() {
        FakeModelClient model = new FakeModelClient("OUTDATED");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("Build uses Gradle", 0.5));
        // 0.5 * 0.2 = 0.1, below floor of 0.3
        List<MemoryItem> result = v.verify(items, "pom.xml exists", 0.3);

        assertTrue(result.isEmpty(), "outdated fact below floor should be dropped");
    }

    @Test
    void unverifiableFactGetsMildPenalty() {
        FakeModelClient model = new FakeModelClient("UNVERIFIABLE");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("User likes oolong tea", 0.9));
        List<MemoryItem> result = v.verify(items, "only code context here", 0.3);

        assertEquals(1, result.size());
        assertEquals(0.9 * 0.7, result.get(0).confidence(), 1e-9);
    }

    @Test
    void respectsMaxFactsLimit() {
        FakeModelClient model = new FakeModelClient("CONFIRMED");
        FactVerifier v = new FactVerifier(model, 2, 0.2, 0.7);

        List<MemoryItem> items = List.of(
                fact("a", 0.9), fact("b", 0.9), fact("c", 0.9), fact("d", 0.9));
        v.verify(items, "context", 0.0);

        // Only first 2 should have been checked.
        assertEquals(2, model.invocations.get());
    }

    @Test
    void passThroughWhenNoModel() {
        FactVerifier v = FactVerifier.passThrough();
        List<MemoryItem> items = List.of(fact("anything", 0.9));
        List<MemoryItem> result = v.verify(items, "context", 0.3);
        assertEquals(1, result.size());
        assertEquals(0.9, result.get(0).confidence(), 1e-9);
    }

    @Test
    void passThroughWhenContextEmpty() {
        FakeModelClient model = new FakeModelClient("OUTDATED");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("a", 0.9));
        List<MemoryItem> result = v.verify(items, "", 0.3);

        assertEquals(1, result.size(), "empty context → skip verification");
        assertEquals(0, model.invocations.get());
    }

    @Test
    void toleratesModelException() {
        FakeModelClient model = new FakeModelClient(p -> { throw new RuntimeException("boom"); });
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("fragile fact", 0.9));
        List<MemoryItem> result = v.verify(items, "context", 0.3);

        // Model failure → UNVERIFIABLE → 0.9 * 0.7 = 0.63, above floor
        assertEquals(1, result.size());
        assertEquals(0.9 * 0.7, result.get(0).confidence(), 1e-9);
    }

    @Test
    void garbageModelOutputTreatedAsUnverifiable() {
        FakeModelClient model = new FakeModelClient("This is some random prose");
        FactVerifier v = new FactVerifier(model, 5, 0.2, 0.7);

        List<MemoryItem> items = List.of(fact("a", 0.9));
        List<MemoryItem> result = v.verify(items, "context", 0.3);
        assertEquals(0.9 * 0.7, result.get(0).confidence(), 1e-9);
    }

    // ---------- helpers ----------

    private static MemoryItem fact(String content, double confidence) {
        Instant now = Instant.now();
        return new MemoryItem("id-" + content.hashCode(), MemoryItem.Tier.SEMANTIC,
                content, null, Map.of(), "test", confidence,
                now, now, 0, content.length() / 4);
    }
}
