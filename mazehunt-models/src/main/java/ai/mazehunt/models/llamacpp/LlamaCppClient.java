package ai.mazehunt.models.llamacpp;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

/**
 * Adapter for the llama.cpp {@code server} binary (OpenAI-compatible endpoint).
 * Point {@code endpoint} at {@code http://localhost:8080/v1}.
 */
public final class LlamaCppClient implements ModelClient {

    private final String endpoint;
    private final String model;
    private final ModelCapability capability;

    public LlamaCppClient(String endpoint, String model, int contextTokens) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.model = model;
        this.capability = new ModelCapability(
                model, "llama.cpp", Set.of(Modality.TEXT), Set.of(Modality.TEXT),
                contextTokens, false, true, 0, 0, 300);
    }

    @Override public ModelCapability capability() { return capability; }

    @Override
    public ModelResponse complete(ModelRequest request) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message m : request.messages()) {
            msgs.add(Map.of(
                    "role", m.role().name().toLowerCase(Locale.ROOT),
                    "content", m.text()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("stream", false);
        JsonNode resp = HttpJson.post(endpoint + "/chat/completions", body,
                Map.of(), Duration.ofMinutes(5));
        String text = resp.path("choices").path(0).path("message").path("content").asText("");
        int prompt = resp.path("usage").path("prompt_tokens").asInt(0);
        int completion = resp.path("usage").path("completion_tokens").asInt(0);
        return new ModelResponse(text, List.of(),
                new ModelResponse.Usage(prompt, completion), "stop");
    }
}
