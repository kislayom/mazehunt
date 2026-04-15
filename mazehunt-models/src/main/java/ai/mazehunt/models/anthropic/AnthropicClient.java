package ai.mazehunt.models.anthropic;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

/** Claude Messages API adapter. */
public final class AnthropicClient implements ModelClient {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final ModelCapability capability;

    public AnthropicClient(String endpoint, String apiKey, String model,
                           int contextTokens, double usdPerMInput, double usdPerMOutput) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.capability = new ModelCapability(
                model, "anthropic",
                Set.of(Modality.TEXT, Modality.IMAGE), Set.of(Modality.TEXT),
                contextTokens, true, false, usdPerMInput, usdPerMOutput, 900);
    }

    @Override public ModelCapability capability() { return capability; }

    @Override
    public ModelResponse complete(ModelRequest request) {
        String system = null;
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message m : request.messages()) {
            if (m.role() == Message.Role.SYSTEM) {
                system = (system == null ? "" : system + "\n") + m.text();
                continue;
            }
            List<Map<String, Object>> content = new ArrayList<>();
            for (Message.Part p : m.parts()) {
                if (p instanceof Message.TextPart tp) {
                    content.add(Map.of("type", "text", "text", tp.text()));
                } else if (p instanceof Message.ImagePart ip) {
                    content.add(Map.of("type", "image",
                            "source", Map.of(
                                    "type", "base64",
                                    "media_type", ip.mimeType(),
                                    "data", Base64.getEncoder().encodeToString(ip.bytes()))));
                }
            }
            msgs.add(Map.of(
                    "role", m.role() == Message.Role.ASSISTANT ? "assistant" : "user",
                    "content", content));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("max_tokens", request.options() == null
                ? 4096 : (int) request.options().getOrDefault("max_tokens", 4096));
        if (system != null) body.put("system", system);

        JsonNode resp = HttpJson.post(endpoint + "/v1/messages", body, Map.of(
                "x-api-key", apiKey,
                "anthropic-version", "2023-06-01"), Duration.ofMinutes(5));
        StringBuilder text = new StringBuilder();
        for (JsonNode c : resp.path("content")) {
            if ("text".equals(c.path("type").asText())) text.append(c.path("text").asText());
        }
        int prompt = resp.path("usage").path("input_tokens").asInt(0);
        int completion = resp.path("usage").path("output_tokens").asInt(0);
        return new ModelResponse(text.toString(), List.of(),
                new ModelResponse.Usage(prompt, completion),
                resp.path("stop_reason").asText("stop"));
    }
}
