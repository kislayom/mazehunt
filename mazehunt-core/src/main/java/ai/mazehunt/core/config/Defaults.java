package ai.mazehunt.core.config;

import java.util.List;
import java.util.Map;

/**
 * Built-in profiles. The {@code workstation-48gb} profile assumes a local Ollama
 * server hosting a 70B-class reasoning model (Qwen, DeepSeek, Llama), a fast
 * small model (Gemma 2), a vision model and an embedding model — with optional
 * OpenAI / Anthropic fall-through for rare over-context tasks.
 */
public final class Defaults {

    private Defaults() {}

    public static MazehuntConfig workstation48gb() {
        return new MazehuntConfig(
                new MazehuntConfig.Hardware("workstation-48gb", 48, 192),
                List.of(
                        new MazehuntConfig.ModelCfg(
                                "qwen2.5:72b", "ollama", "http://localhost:11434",
                                null, "reasoning", 32768, true,
                                Map.of("num_ctx", 32768, "num_gpu", 99)),
                        new MazehuntConfig.ModelCfg(
                                "deepseek-r1:70b", "ollama", "http://localhost:11434",
                                null, "reasoning", 32768, true, Map.of()),
                        new MazehuntConfig.ModelCfg(
                                "gemma2:9b", "ollama", "http://localhost:11434",
                                null, "fast", 8192, true, Map.of()),
                        new MazehuntConfig.ModelCfg(
                                "llava:13b", "ollama", "http://localhost:11434",
                                null, "vision", 4096, true, Map.of()),
                        new MazehuntConfig.ModelCfg(
                                "nomic-embed-text", "ollama", "http://localhost:11434",
                                null, "embedding", 8192, true, Map.of()),
                        new MazehuntConfig.ModelCfg(
                                "gpt-4o", "openai", "https://api.openai.com/v1",
                                "OPENAI_API_KEY", "reasoning", 128000, false, Map.of()),
                        new MazehuntConfig.ModelCfg(
                                "claude-opus-4-6", "anthropic", "https://api.anthropic.com",
                                "ANTHROPIC_API_KEY", "reasoning", 200000, false, Map.of())
                ),
                new MazehuntConfig.Router(
                        "qwen2.5:72b", "gemma2:9b", "nomic-embed-text",
                        "llava:13b", "whisper-1",
                        true, 28000, 8),
                new MazehuntConfig.Memory(
                        "local", "http://localhost:8765",
                        "./data/memory.db", 4000, 1200, 12, 0.35),
                new MazehuntConfig.Proactive(true, 3, true),
                Map.of()
        );
    }
}
