package ai.mazehunt.core.util;

/** SQLite FTS5 helpers. */
public final class Fts {

    private Fts() {}

    /**
     * Turn free-form user input into a safe FTS5 MATCH expression.
     * Non-alphanumerics become spaces, short tokens are dropped, remaining
     * tokens are quoted and OR-joined.
     */
    public static String sanitiseMatch(String raw) {
        if (raw == null) return "\"\"";
        StringBuilder cleaned = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            cleaned.append(Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' ? c : ' ');
        }
        String[] terms = cleaned.toString().trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String t : terms) {
            if (t.length() < 2) continue;
            if (out.length() > 0) out.append(" OR ");
            out.append('"').append(t).append('"');
        }
        return out.length() == 0 ? "\"\"" : out.toString();
    }
}
