package ai.mazehunt.api.model;

import java.util.List;

/** Embeds strings into float vectors. Contract: {@link #dimensions()} is stable. */
public interface EmbeddingClient {
    int dimensions();
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
