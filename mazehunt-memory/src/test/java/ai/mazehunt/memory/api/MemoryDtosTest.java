package ai.mazehunt.memory.api;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.core.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MemoryDtosTest {

    @Test
    void itemDtoRoundTripsThroughJson() {
        MemoryItem original = new MemoryItem("id-1", MemoryItem.Tier.SEMANTIC,
                "content", new float[]{1, 2, 3},  // embedding intentionally stripped on the wire
                Map.of("k", "v"), "src", 0.87,
                Instant.ofEpochMilli(1_700_000_000_000L),
                Instant.ofEpochMilli(1_700_000_123_456L),
                4, 7);

        MemoryDtos.MemoryItemDto dto = MemoryDtos.MemoryItemDto.fromDomain(original);
        String json = Json.stringify(dto);
        MemoryDtos.MemoryItemDto parsed = Json.parse(json, MemoryDtos.MemoryItemDto.class);
        MemoryItem restored = parsed.toDomain();

        assertEquals(original.id(), restored.id());
        assertEquals(original.tier(), restored.tier());
        assertEquals(original.content(), restored.content());
        assertEquals(original.tags(), restored.tags());
        assertEquals(original.source(), restored.source());
        assertEquals(original.confidence(), restored.confidence(), 1e-9);
        assertEquals(original.createdAt(), restored.createdAt());
        assertEquals(original.lastAccessedAt(), restored.lastAccessedAt());
        assertEquals(original.accessCount(), restored.accessCount());
        assertEquals(original.tokens(), restored.tokens());
        assertNull(restored.embedding(), "embedding must not travel across the wire");
    }

    @Test
    void itemDtoJsonUsesEpochMillis() {
        MemoryItem m = new MemoryItem("id-1", MemoryItem.Tier.EPISODIC, "hello",
                null, Map.of(), "t", 0.9,
                Instant.ofEpochMilli(42L), Instant.ofEpochMilli(99L), 0, 2);
        JsonNode tree = Json.tree(Json.stringify(MemoryDtos.MemoryItemDto.fromDomain(m)));
        // Confirm wire format avoids jsr310-style ISO strings.
        assertEquals(42L, tree.path("createdAtMillis").asLong());
        assertEquals(99L, tree.path("lastAccessedAtMillis").asLong());
        assertFalse(tree.has("embedding"));
    }

    @Test
    void recallRequestDtoNullFieldsStayNull() {
        MemoryDtos.RecallRequestDto req = Json.parse(
                "{\"query\":\"foo\"}", MemoryDtos.RecallRequestDto.class);
        assertEquals("foo", req.query());
        assertNull(req.tokenBudget());
        assertNull(req.tiers());
        assertNull(req.requiredTags());
        assertNull(req.minConfidence());
    }

    @Test
    void recallRequestDtoSerialisesTiersAsEnumNames() {
        MemoryDtos.RecallRequestDto req = new MemoryDtos.RecallRequestDto(
                "q", 500, Set.of(MemoryItem.Tier.SEMANTIC), Map.of(), 0.5);
        JsonNode tree = Json.tree(Json.stringify(req));
        assertTrue(tree.path("tiers").isArray());
        assertEquals("SEMANTIC", tree.path("tiers").get(0).asText());
    }

    @Test
    void idResponseAndErrorResponseRoundTrip() {
        assertEquals("abc",
                Json.parse(Json.stringify(new MemoryDtos.IdResponse("abc")),
                        MemoryDtos.IdResponse.class).id());
        assertEquals("boom",
                Json.parse(Json.stringify(new MemoryDtos.ErrorResponseDto("boom")),
                        MemoryDtos.ErrorResponseDto.class).error());
    }
}
