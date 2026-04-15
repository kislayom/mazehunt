package ai.mazehunt.core.memory;

import java.util.List;

public interface VectorIndex {
    void add(String id, float[] vector);
    void remove(String id);
    int size();
    List<Hit> topK(float[] query, int k);

    record Hit(String id, double score) {}
}
