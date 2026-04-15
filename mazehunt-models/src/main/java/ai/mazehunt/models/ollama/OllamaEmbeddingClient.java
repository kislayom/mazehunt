package ai.mazehunt.models.ollama;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.*;
import ai.mazehunt.core.util.Http;
import ai.mazehunt.core.util.Vectors;
import ai.mazehunt.models.http.HttpJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;

/** {@link EmbeddingClient} backed by Ollama's {@code /api/embeddings} endpoint. */
public final class OllamaEmbeddingClient implements EmbeddingClient, ModelClient {

    private final String endpoint;
    private final String model;
    private final ModelCapability capability;
    private volatile int dims = -1;

    public OllamaEmbeddingClient(String endpoint, String model) {
        this.endpoint = Http.normaliseEndpoint(endpoint);
        this.model = model;
        this.capability = new ModelCapability(
                model, "ollama", Set.of(Modality.TEXT), Set.of(Modality.EMBEDDING),
                8192, false, true, 0, 0, 50);
    }

    @Override public ModelCapability capability() { return capability; }

    @Override
    public int dimensions() {
        if (dims < 0) embed("probe");
        return dims;
    }

    @Override
    public float[] embed(String text) {
        JsonNode resp = HttpJson.post(endpoint + "/api/embeddings",
                Map.of("model", model, "prompt", text == null ? "" : text),
                Map.of(), Duration.ofSeconds(30));
        float[] v = Vectors.fromJsonArray(resp.path("embedding"));
        dims = v.length;
        return v;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) out.add(embed(t));
        return out;
    }

    @Override
    public ModelResponse complete(ModelRequest request) {
        throw new UnsupportedOperationException("Embedding model does not support chat completion");
    }
}
