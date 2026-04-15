package ai.mazehunt.models.openai;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

public final class OpenAiEmbeddingClient implements EmbeddingClient, ModelClient {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final ModelCapability capability;
    private volatile int dims = -1;

    public OpenAiEmbeddingClient(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.capability = new ModelCapability(model, "openai",
                Set.of(Modality.TEXT), Set.of(Modality.EMBEDDING),
                8192, false, false, 0.02, 0, 400);
    }

    @Override public ModelCapability capability() { return capability; }
    @Override public int dimensions() { if (dims < 0) embed("probe"); return dims; }

    @Override
    public float[] embed(String text) {
        JsonNode resp = HttpJson.post(endpoint + "/embeddings",
                Map.of("model", model, "input", text == null ? "" : text),
                Map.of("authorization", "Bearer " + apiKey), Duration.ofSeconds(30));
        JsonNode arr = resp.path("data").path(0).path("embedding");
        float[] v = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) v[i] = (float) arr.get(i).asDouble();
        dims = v.length;
        return v;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        JsonNode resp = HttpJson.post(endpoint + "/embeddings",
                Map.of("model", model, "input", texts),
                Map.of("authorization", "Bearer " + apiKey), Duration.ofSeconds(60));
        JsonNode data = resp.path("data");
        List<float[]> out = new ArrayList<>(data.size());
        for (JsonNode d : data) {
            JsonNode arr = d.path("embedding");
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) v[i] = (float) arr.get(i).asDouble();
            dims = v.length;
            out.add(v);
        }
        return out;
    }

    @Override
    public ModelResponse complete(ModelRequest request) {
        throw new UnsupportedOperationException();
    }
}
