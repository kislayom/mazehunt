package ai.mazehunt.api.memory;

import java.util.Map;
import java.util.Set;

/**
 * Parameters for {@link MemoryService#recall(RecallRequest)}.
 *
 * @param query         natural-language question or topic
 * @param tokenBudget   cap on total tokens of returned items; ranker prunes aggressively
 * @param tiers         which tiers to consult ({@code null} = all)
 * @param requiredTags  only return items carrying these tags
 * @param minConfidence drop items below this confidence (0..1)
 */
public record RecallRequest(
        String query,
        int tokenBudget,
        Set<MemoryItem.Tier> tiers,
        Map<String, String> requiredTags,
        double minConfidence
) {
    public static RecallRequest of(String query, int tokenBudget) {
        return new RecallRequest(query, tokenBudget, null, Map.of(), 0.0);
    }
}
