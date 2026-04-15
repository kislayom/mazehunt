package ai.mazehunt.memory.testsupport;

import ai.mazehunt.api.model.EmbeddingClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic bag-of-keywords embedder for unit tests. Texts that share more
 * keywords from {@code vocabulary} end up with higher cosine similarity — so
 * test expectations about "similar-to-X" have a stable ground truth without
 * any real model.
 */
public final class FakeEmbedder implements EmbeddingClient {

    private final List<String> vocab;

    public FakeEmbedder(List<String> vocabulary) {
        this.vocab = List.copyOf(vocabulary);
    }

    @Override public int dimensions() { return vocab.size(); }

    @Override
    public float[] embed(String text) {
        float[] v = new float[vocab.size()];
        if (text == null) return v;
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < vocab.size(); i++) {
            if (lower.contains(vocab.get(i))) v[i] = 1.0f;
        }
        return v;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) out.add(embed(t));
        return out;
    }
}
