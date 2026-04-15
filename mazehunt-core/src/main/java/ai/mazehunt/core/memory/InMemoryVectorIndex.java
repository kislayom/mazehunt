package ai.mazehunt.core.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.core.util.Vectors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple but fast in-memory brute-force cosine index. For 192 GB RAM hosts this
 * easily scales to millions of 768-dim vectors (~3 GB per 1 M). When the corpus
 * outgrows RAM we can swap this with a pluggable HNSW implementation behind the
 * {@link VectorIndex} interface.
 */
public final class InMemoryVectorIndex implements VectorIndex {

    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();

    @Override
    public void add(String id, float[] vector) {
        if (vector != null) vectors.put(id, vector);
    }

    @Override
    public void remove(String id) {
        vectors.remove(id);
    }

    @Override
    public int size() { return vectors.size(); }

    @Override
    public List<Hit> topK(float[] query, int k) {
        if (query == null || vectors.isEmpty()) return List.of();
        PriorityQueue<Hit> heap = new PriorityQueue<>(Comparator.comparingDouble(Hit::score));
        for (Map.Entry<String, float[]> e : vectors.entrySet()) {
            double s = Vectors.cosine(query, e.getValue());
            if (heap.size() < k) {
                heap.offer(new Hit(e.getKey(), s));
            } else if (heap.peek().score() < s) {
                heap.poll();
                heap.offer(new Hit(e.getKey(), s));
            }
        }
        List<Hit> out = new ArrayList<>(heap);
        out.sort(Comparator.comparingDouble(Hit::score).reversed());
        return out;
    }

    /** Warm from an existing store on startup. */
    public void hydrate(Iterable<MemoryItem> items) {
        for (MemoryItem m : items) {
            if (m.embedding() != null) vectors.put(m.id(), m.embedding());
        }
    }
}
