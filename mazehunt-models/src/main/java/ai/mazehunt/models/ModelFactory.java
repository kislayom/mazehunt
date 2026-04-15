package ai.mazehunt.models;

import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.core.config.MazehuntConfig;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.models.anthropic.AnthropicClient;
import ai.mazehunt.models.llamacpp.LlamaCppClient;
import ai.mazehunt.models.ollama.OllamaClient;
import ai.mazehunt.models.ollama.OllamaEmbeddingClient;
import ai.mazehunt.models.openai.OpenAiClient;
import ai.mazehunt.models.openai.OpenAiEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Builds {@link ModelClient}s from {@link MazehuntConfig} entries. */
public final class ModelFactory {

    private static final Logger log = LoggerFactory.getLogger(ModelFactory.class);

    private ModelFactory() {}

    /** Register every configured model with the router under its declared role. */
    public static void registerAll(MazehuntConfig cfg, ModelRouter router) {
        for (MazehuntConfig.ModelCfg m : cfg.models()) {
            try {
                ModelClient client = build(m);
                router.register(mapRole(m.role()), client);
            } catch (RuntimeException e) {
                log.warn("Skipping model {} ({}): {}", m.id(), m.provider(), e.toString());
            }
        }
    }

    public static ModelClient build(MazehuntConfig.ModelCfg m) {
        String key = m.apiKeyEnv() == null ? null : System.getenv(m.apiKeyEnv());
        Map<String, Object> opts = m.options() == null ? Map.of() : m.options();
        return switch (m.provider()) {
            case "ollama" -> "embedding".equals(m.role())
                    ? new OllamaEmbeddingClient(m.endpoint(), m.id())
                    : new OllamaClient(m.endpoint(), m.id(), m.contextTokens(),
                                       "vision".equals(m.role()), opts);
            case "openai" -> {
                if (key == null) throw new IllegalStateException("missing env " + m.apiKeyEnv());
                yield "embedding".equals(m.role())
                        ? new OpenAiEmbeddingClient(m.endpoint(), key, m.id())
                        : new OpenAiClient(m.endpoint(), key, m.id(), m.contextTokens(),
                                           "vision".equals(m.role()), 5.0, 15.0);
            }
            case "anthropic" -> {
                if (key == null) throw new IllegalStateException("missing env " + m.apiKeyEnv());
                yield new AnthropicClient(m.endpoint(), key, m.id(), m.contextTokens(), 15.0, 75.0);
            }
            case "llama.cpp", "llamacpp" -> new LlamaCppClient(m.endpoint(), m.id(), m.contextTokens());
            default -> throw new IllegalArgumentException("unknown provider: " + m.provider());
        };
    }

    private static ModelRouter.Role mapRole(String role) {
        return switch (role == null ? "" : role) {
            case "embedding" -> ModelRouter.Role.EMBEDDING;
            case "vision"    -> ModelRouter.Role.VISION;
            case "audio"     -> ModelRouter.Role.AUDIO;
            case "fast"      -> ModelRouter.Role.FAST;
            default          -> ModelRouter.Role.REASONING;
        };
    }

    /** Null-safe check — used by hosts that want to know whether an embedder exists. */
    public static boolean isEmbedder(ModelClient c) { return c instanceof EmbeddingClient; }
}
