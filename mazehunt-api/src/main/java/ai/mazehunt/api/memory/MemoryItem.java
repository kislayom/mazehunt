package ai.mazehunt.api.memory;

import java.time.Instant;
import java.util.Map;

/**
 * A single fact, utterance, observation or procedure stored by the memory tier.
 *
 * <p>{@code confidence} and {@code source} are the anti-hallucination scaffolding:
 * only facts with a source are fed back to the model as "ground truth"; lower-
 * confidence items are either re-verified or flagged UNKNOWN.
 */
public record MemoryItem(
        String id,
        Tier tier,
        String content,
        float[] embedding,
        Map<String, String> tags,
        String source,
        double confidence,
        Instant createdAt,
        Instant lastAccessedAt,
        int accessCount,
        /** Token count of {@link #content} (pre-computed for budgeting). */
        int tokens
) {
    public enum Tier {
        /** Most recent turns; always in the prompt. */
        WORKING,
        /** Raw timestamped events (conversations, tool results). */
        EPISODIC,
        /** Consolidated facts / profile / preferences. */
        SEMANTIC,
        /** Learned skills, routines, heuristics. */
        PROCEDURAL
    }
}
