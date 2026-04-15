package ai.mazehunt.core.util;

/**
 * Heuristic token counter. Good enough for budgeting (within ~10% of BPE for
 * English prose). Swap in a real BPE tokenizer by dropping a class implementing
 * the same static API onto the classpath — the memory code only calls {@link #count(String)}.
 */
public final class Tokens {
    private Tokens() {}

    /** ~4 chars per token for English; also clamps at word count as a floor. */
    public static int count(String s) {
        if (s == null || s.isEmpty()) return 0;
        int chars = s.length();
        int words = 1;
        for (int i = 0; i < chars; i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t') words++;
        }
        return Math.max(words, Math.round(chars / 4.0f));
    }
}
