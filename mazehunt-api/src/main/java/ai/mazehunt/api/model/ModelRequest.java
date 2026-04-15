package ai.mazehunt.api.model;

import java.util.List;
import java.util.Map;

/** Provider-agnostic model request. */
public record ModelRequest(
        List<Message> messages,
        List<ToolSpec> tools,
        Map<String, Object> options
) {
    public static ModelRequest of(List<Message> messages) {
        return new ModelRequest(messages, List.of(), Map.of());
    }
}
