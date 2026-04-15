package ai.mazehunt.core.config;

import java.util.List;
import java.util.Map;

/** Top-level YAML-bindable configuration. */
public record MazehuntConfig(
        Hardware hardware,
        List<ModelCfg> models,
        Router router,
        Memory memory,
        Proactive proactive,
        Map<String, Object> skills
) {
    public record Hardware(String profile, int gpuVramGb, int systemRamGb) {}

    public record ModelCfg(
            String id,
            String provider,
            String endpoint,
            String apiKeyEnv,
            String role,           // "reasoning", "fast", "embedding", "vision", "audio"
            int contextTokens,
            boolean local,
            Map<String, Object> options
    ) {}

    public record Router(
            String defaultReasoning,
            String defaultFast,
            String defaultEmbedding,
            String defaultVision,
            String defaultAudio,
            boolean preferLocal,
            /** Fall through to cloud only if the task exceeds these thresholds. */
            int cloudFallbackMinContext,
            int cloudFallbackComplexity
    ) {}

    public record Memory(
            String sqlitePath,
            int workingTokenBudget,
            int recallDefaultBudget,
            int consolidateEveryNTurns,
            double confidenceFloor
    ) {}

    public record Proactive(
            boolean enabled,
            int maxNudgesPerHour,
            boolean allowBackgroundResearch
    ) {}
}
