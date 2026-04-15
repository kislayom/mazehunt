package ai.mazehunt.api.model;

import ai.mazehunt.api.Modality;
import java.util.Set;

/**
 * Static description of what a model can do. Used by the router to pick the
 * best candidate for a given task (cost / latency / modality / context length).
 */
public record ModelCapability(
        String id,
        String provider,
        Set<Modality> inputs,
        Set<Modality> outputs,
        int maxContextTokens,
        boolean supportsTools,
        boolean isLocal,
        /** USD per 1M input tokens (0 for local). */
        double usdPerMInput,
        double usdPerMOutput,
        /** ~p50 time-to-first-token in ms on the reference hardware. */
        int latencyHintMs
) {}
