package ai.mazehunt.core.router;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.ModelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks a {@link ModelClient} for a given role and task envelope. Local models
 * win by default; cloud is only chosen when the task overflows local context
 * or requires capabilities (e.g. certain vision quality) the local models lack.
 */
public final class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    public enum Role { REASONING, FAST, VISION, AUDIO, EMBEDDING }

    public record Task(int approxInputTokens, int complexity, Modality input) {
        public static Task light() { return new Task(500, 1, Modality.TEXT); }
        public static Task heavy() { return new Task(8000, 8, Modality.TEXT); }
    }

    private final Map<Role, List<ModelClient>> byRole = new EnumMap<>(Role.class);
    private final Map<String, ModelClient> byId = new ConcurrentHashMap<>();
    private final boolean preferLocal;
    private final int cloudFallbackMinContext;
    private final int cloudFallbackComplexity;

    public ModelRouter(boolean preferLocal, int cloudFallbackMinContext, int cloudFallbackComplexity) {
        this.preferLocal = preferLocal;
        this.cloudFallbackMinContext = cloudFallbackMinContext;
        this.cloudFallbackComplexity = cloudFallbackComplexity;
        for (Role r : Role.values()) byRole.put(r, new ArrayList<>());
    }

    public void register(Role role, ModelClient client) {
        byRole.get(role).add(client);
        byId.put(client.capability().id(), client);
        log.info("Registered {} model: {}", role, client.capability().id());
    }

    public Optional<ModelClient> byId(String id) { return Optional.ofNullable(byId.get(id)); }

    public ModelClient pick(Role role, Task task) {
        List<ModelClient> candidates = byRole.getOrDefault(role, List.of());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No model registered for role " + role);
        }
        boolean forceCloud = task.approxInputTokens > cloudFallbackMinContext
                          || task.complexity    > cloudFallbackComplexity;

        return candidates.stream()
                .filter(m -> m.capability().maxContextTokens() >= task.approxInputTokens)
                .sorted(Comparator.comparingDouble(m -> cost(m, task, forceCloud)))
                .findFirst()
                .orElse(candidates.get(0));
    }

    private double cost(ModelClient m, Task t, boolean forceCloud) {
        var c = m.capability();
        double base = c.isLocal() ? 0.0
                : (c.usdPerMInput() * t.approxInputTokens() / 1_000_000.0);
        // Penalise cloud when preferring local, unless forced.
        double localityPenalty = preferLocal && !c.isLocal() && !forceCloud ? 10.0 : 0.0;
        double latency = c.latencyHintMs() / 10000.0;
        return base + localityPenalty + latency;
    }
}
