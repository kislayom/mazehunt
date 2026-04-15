package ai.mazehunt.api.skill;

import java.util.Map;

/**
 * Result of a tool invocation. Carries {@code citations} so the agent can
 * attribute any downstream claim to an externally verifiable source.
 */
public record SkillResult(
        boolean success,
        Object value,
        String errorMessage,
        Map<String, Object> metadata,
        java.util.List<String> citations
) {
    public static SkillResult ok(Object value) {
        return new SkillResult(true, value, null, Map.of(), java.util.List.of());
    }
    public static SkillResult ok(Object value, java.util.List<String> citations) {
        return new SkillResult(true, value, null, Map.of(), citations);
    }
    public static SkillResult error(String message) {
        return new SkillResult(false, null, message, Map.of(), java.util.List.of());
    }
}
