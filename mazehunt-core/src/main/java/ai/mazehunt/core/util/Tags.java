package ai.mazehunt.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serialisation format for {@code Map&lt;String,String&gt;} tag sets.
 *
 * <p>Format: {@code k1=v1;k2=v2}. Semicolons and equals signs inside keys /
 * values are stripped (we do not need round-trip fidelity — tags are small
 * controlled labels).
 */
public final class Tags {

    private Tags() {}

    public static String encode(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        tags.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(stripDelims(k)).append('=').append(v == null ? "" : stripDelims(v));
        });
        return sb.toString();
    }

    public static Map<String, String> decode(String s) {
        if (s == null || s.isEmpty()) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (String piece : s.split(";")) {
            int eq = piece.indexOf('=');
            if (eq > 0) out.put(piece.substring(0, eq), piece.substring(eq + 1));
        }
        return out;
    }

    private static String stripDelims(String v) { return v.replace(";", "").replace("=", ""); }
}
