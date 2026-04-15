package ai.mazehunt.api.model;

import java.util.Map;

/** JSON-Schema-style description of a tool the model may call. */
public record ToolSpec(String name, String description, Map<String, Object> parametersSchema) {}
