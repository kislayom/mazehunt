package ai.mazehunt.api.memory;

import java.util.List;
import java.util.Map;

/**
 * Central memory facade. The agent only ever talks to this interface; the
 * underlying tiers (episodic log, vector index, knowledge graph) are free to
 * evolve behind it.
 */
public interface MemoryService {

    /** Append a raw event to the episodic tier. Embedding & tagging happen async. */
    String remember(String content, Map<String, String> tags, String source);

    /**
     * Store a distilled fact in the semantic tier. The service deduplicates
     * against existing semantic items; contradictions are logged for later
     * reconciliation rather than silently overwritten.
     */
    String learnFact(String fact, String source, double confidence);

    /** Retrieve a ranked, token-budgeted set of items relevant to a query. */
    List<MemoryItem> recall(RecallRequest request);

    /** Force a consolidation pass: summarise episodic → semantic, evict WORKING. */
    void consolidate();

    /** Current counts per tier (observability). */
    Map<MemoryItem.Tier, Long> stats();
}
