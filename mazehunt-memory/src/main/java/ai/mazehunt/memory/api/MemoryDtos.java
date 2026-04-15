package ai.mazehunt.memory.api;

import ai.mazehunt.api.memory.MemoryItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Timestamps are carried as epoch-millis to avoid pulling jackson-datatype-jsr310
// into the memory service's dependency tree. Clients convert back to Instant.

/**
 * Wire-format records for the memory HTTP API. Kept deliberately flat and
 * free of framework annotations so Jackson serialises them via record
 * components without any reflection gymnastics.
 *
 * <p>Note: we do NOT send embeddings over the wire — they are an internal
 * implementation detail of the service and can be hundreds of KB per item.
 * Clients supply only {@code content}; the service computes the vector.
 */
public final class MemoryDtos {

    private MemoryDtos() {}

    public record RememberRequestDto(String content, Map<String, String> tags, String source) {}
    public record LearnFactRequestDto(String content, String source, Double confidence) {}
    public record IdResponse(String id) {}

    public record RecallRequestDto(
            String query,
            Integer tokenBudget,
            Set<MemoryItem.Tier> tiers,
            Map<String, String> requiredTags,
            Double minConfidence
    ) {}

    public record RecallResponseDto(List<MemoryItemDto> items) {}

    /** Same fields as {@link MemoryItem} minus {@code embedding}; timestamps are epoch-millis. */
    public record MemoryItemDto(
            String id,
            MemoryItem.Tier tier,
            String content,
            Map<String, String> tags,
            String source,
            double confidence,
            long createdAtMillis,
            long lastAccessedAtMillis,
            int accessCount,
            int tokens
    ) {
        public static MemoryItemDto fromDomain(MemoryItem m) {
            return new MemoryItemDto(m.id(), m.tier(), m.content(), m.tags(), m.source(),
                    m.confidence(),
                    m.createdAt().toEpochMilli(), m.lastAccessedAt().toEpochMilli(),
                    m.accessCount(), m.tokens());
        }

        public MemoryItem toDomain() {
            return new MemoryItem(id, tier, content, null, tags, source, confidence,
                    Instant.ofEpochMilli(createdAtMillis),
                    Instant.ofEpochMilli(lastAccessedAtMillis),
                    accessCount, tokens);
        }
    }

    public record StatsResponseDto(Map<MemoryItem.Tier, Long> counts) {}
    public record HealthResponseDto(String status, long uptimeMs) {}
    public record ErrorResponseDto(String error) {}
}
