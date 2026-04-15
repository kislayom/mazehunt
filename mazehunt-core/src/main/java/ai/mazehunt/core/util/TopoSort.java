package ai.mazehunt.core.util;

import java.util.*;
import java.util.function.Function;

/** Kahn's algorithm. Returns declaration order on cycles. */
public final class TopoSort {

    private TopoSort() {}

    public static <T> List<T> sort(Collection<T> nodes,
                                   Function<T, String> idOf,
                                   Function<T, Collection<String>> depsOf) {
        Map<String, T> byId = new LinkedHashMap<>();
        Map<String, List<String>> adj = new LinkedHashMap<>();
        Map<String, Integer> indeg = new LinkedHashMap<>();
        for (T n : nodes) {
            String id = idOf.apply(n);
            byId.put(id, n);
            adj.putIfAbsent(id, new ArrayList<>());
            indeg.putIfAbsent(id, 0);
        }
        for (T n : nodes) {
            String id = idOf.apply(n);
            Collection<String> deps = depsOf.apply(n);
            if (deps == null) continue;
            for (String d : deps) {
                if (!byId.containsKey(d)) continue;
                adj.get(d).add(id);
                indeg.merge(id, 1, Integer::sum);
            }
        }
        Deque<String> q = new ArrayDeque<>();
        indeg.forEach((k, v) -> { if (v == 0) q.add(k); });
        List<T> out = new ArrayList<>(byId.size());
        while (!q.isEmpty()) {
            String cur = q.poll();
            out.add(byId.get(cur));
            for (String next : adj.get(cur)) {
                if (indeg.merge(next, -1, Integer::sum) == 0) q.add(next);
            }
        }
        if (out.size() != byId.size()) {
            out.clear();
            out.addAll(byId.values());
        }
        return out;
    }
}
