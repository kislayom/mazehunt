package ai.mazehunt.api.model;

import java.util.List;

/** Provider-agnostic model response. */
public record ModelResponse(
        String text,
        List<ToolCall> toolCalls,
        Usage usage,
        String finishReason
) {
    public static ModelResponse text(String text, Usage usage) {
        return new ModelResponse(text, List.of(), usage, "stop");
    }

    public record Usage(int promptTokens, int completionTokens) {
        public int total() { return promptTokens + completionTokens; }
        public static Usage zero() { return new Usage(0, 0); }
    }
}
