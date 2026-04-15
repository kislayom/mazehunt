package ai.mazehunt.models.openai;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.core.util.Http;
import ai.mazehunt.core.util.Images;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

/** Chat-completions client for OpenAI-compatible endpoints. */
public final class OpenAiClient implements ModelClient {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final ModelCapability capability;

    public OpenAiClient(String endpoint, String apiKey, String model,
                        int contextTokens, boolean vision,
                        double usdPerMInput, double usdPerMOutput) {
        this.endpoint = Http.normaliseEndpoint(endpoint);
        this.apiKey = apiKey;
        this.model = model;
        Set<Modality> inputs = vision
                ? Set.of(Modality.TEXT, Modality.IMAGE) : Set.of(Modality.TEXT);
        this.capability = new ModelCapability(
                model, "openai", inputs, Set.of(Modality.TEXT),
                contextTokens, true, false, usdPerMInput, usdPerMOutput, 800);
    }

    @Override public ModelCapability capability() { return capability; }

    @Override
    public ModelResponse complete(ModelRequest request) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message m : request.messages()) {
            Map<String, Object> jm = new LinkedHashMap<>();
            jm.put("role", m.role().wire());
            List<Map<String, Object>> parts = new ArrayList<>();
            for (Message.Part p : m.parts()) {
                if (p instanceof Message.TextPart tp) {
                    parts.add(Map.of("type", "text", "text", tp.text()));
                } else if (p instanceof Message.ImagePart ip) {
                    parts.add(Map.of("type", "image_url",
                            "image_url", Map.of("url", Images.toDataUri(ip.bytes(), ip.mimeType()))));
                }
            }
            jm.put("content", parts.size() == 1 && parts.get(0).get("type").equals("text")
                    ? parts.get(0).get("text") : parts);
            msgs.add(jm);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        if (request.options() != null) body.putAll(request.options());

        JsonNode resp = HttpJson.post(endpoint + "/chat/completions", body,
                Map.of("authorization", "Bearer " + apiKey), Duration.ofMinutes(5));
        String text = resp.path("choices").path(0).path("message").path("content").asText("");
        int prompt = resp.path("usage").path("prompt_tokens").asInt(0);
        int completion = resp.path("usage").path("completion_tokens").asInt(0);
        return new ModelResponse(text, List.of(),
                new ModelResponse.Usage(prompt, completion),
                resp.path("choices").path(0).path("finish_reason").asText("stop"));
    }
}
