package ai.mazehunt.models.ollama;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.core.util.Http;
import ai.mazehunt.core.util.Images;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

/**
 * Client for a local Ollama server (Gemma, DeepSeek, Qwen, Llama, …).
 * Uses the {@code /api/chat} endpoint; non-streaming for simplicity.
 */
public final class OllamaClient implements ModelClient {

    private final String endpoint;
    private final String model;
    private final ModelCapability capability;
    private final Map<String, Object> extraOptions;

    public OllamaClient(String endpoint, String model, int contextTokens,
                        boolean vision, Map<String, Object> extraOptions) {
        this.endpoint = Http.normaliseEndpoint(endpoint);
        this.model = model;
        this.extraOptions = extraOptions == null ? Map.of() : extraOptions;
        Set<Modality> inputs = vision
                ? Set.of(Modality.TEXT, Modality.IMAGE) : Set.of(Modality.TEXT);
        this.capability = new ModelCapability(
                model, "ollama", inputs, Set.of(Modality.TEXT),
                contextTokens, true, true, 0, 0, 400);
    }

    @Override public ModelCapability capability() { return capability; }

    @Override
    public ModelResponse complete(ModelRequest request) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message m : request.messages()) {
            Map<String, Object> jm = new LinkedHashMap<>();
            jm.put("role", m.role().wire());
            StringBuilder text = new StringBuilder();
            List<String> images = new ArrayList<>();
            for (Message.Part p : m.parts()) {
                if (p instanceof Message.TextPart tp) text.append(tp.text());
                else if (p instanceof Message.ImagePart ip)
                    images.add(Images.toBase64(ip.bytes()));
            }
            jm.put("content", text.toString());
            if (!images.isEmpty()) jm.put("images", images);
            msgs.add(jm);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("stream", false);
        Map<String, Object> options = new LinkedHashMap<>(extraOptions);
        if (request.options() != null) options.putAll(request.options());
        if (!options.isEmpty()) body.put("options", options);

        JsonNode resp = HttpJson.post(endpoint + "/api/chat", body, Map.of(), Duration.ofMinutes(5));
        String text = resp.path("message").path("content").asText("");
        int promptTokens = resp.path("prompt_eval_count").asInt(0);
        int completionTokens = resp.path("eval_count").asInt(0);
        return new ModelResponse(text, List.of(),
                new ModelResponse.Usage(promptTokens, completionTokens),
                resp.path("done_reason").asText("stop"));
    }
}
