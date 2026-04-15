package ai.mazehunt.core.util;

import com.fasterxml.jackson.databind.JsonNode;

/** Best-effort JSON extraction & path navigation for noisy model outputs. */
public final class JsonExtract {

    private JsonExtract() {}

    /**
     * Pulls the outermost {@code { … }} block from a string. Useful when a
     * model wraps its JSON in prose or fences.
     */
    public static String extractObject(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        return (start < 0 || end <= start) ? "{}" : raw.substring(start, end + 1);
    }

    /**
     * Dot-separated field lookup over a {@link java.util.Map} or
     * {@link JsonNode}. Returns the deepest matching value; returns the
     * current node if the path segment does not match.
     */
    public static Object path(Object root, String path) {
        if (root == null) return null;
        if (path == null || path.isEmpty()) return root;
        Object cur = root;
        for (String seg : path.split("\\.")) {
            if (seg.isEmpty()) continue;
            if (cur instanceof java.util.Map<?, ?> map) cur = map.get(seg);
            else if (cur instanceof JsonNode n) cur = n.path(seg);
            else return cur;
        }
        return cur;
    }
}
