package ai.mazehunt.memory.verify;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Self-healing memory: before recalled facts reach the LLM's prompt, verify
 * them against currently known context. Inspired by Claude's approach where
 * a memory entry like "build uses Gradle" is checked against the actual
 * project files before being trusted.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>For each recalled fact, ask a small model: "given this context, is the
 *       following fact CONFIRMED, OUTDATED, or UNVERIFIABLE?"</li>
 *   <li>CONFIRMED facts pass through unchanged.</li>
 *   <li>OUTDATED facts have their confidence demoted (below the floor → dropped).</li>
 *   <li>UNVERIFIABLE facts pass through at reduced confidence (the model
 *       flagging a fact it can't check is honest, not harmful).</li>
 * </ol>
 *
 * <p>The verifier is <b>optional</b>. When no model is configured it becomes a
 * pass-through — every fact survives. The caller's token budget is still the
 * primary defence against context bloat; verification adds a quality filter on
 * top.
 *
 * <h3>Performance</h3>
 * One model call per fact. Limit the number of facts verified per recall
 * (default: top 5) so the hot path doesn't stall.
 */
public final class FactVerifier {

    private static final Logger log = LoggerFactory.getLogger(FactVerifier.class);

    private static final String VERIFY_PROMPT = """
            You are a fact checker. Given the CONTEXT below, evaluate the FACT.
            Reply with exactly one word: CONFIRMED, OUTDATED, or UNVERIFIABLE.

            CONTEXT:
            %s

            FACT:
            %s

            Verdict:""";

    private final ModelClient model;
    private final int maxFactsToVerify;
    private final double outdatedPenalty;
    private final double unverifiablePenalty;

    /**
     * @param model              small fast model for verification (nullable → pass-through)
     * @param maxFactsToVerify   cap per recall to bound latency (default: 5)
     * @param outdatedPenalty    confidence multiplier for OUTDATED facts (e.g. 0.2)
     * @param unverifiablePenalty confidence multiplier for UNVERIFIABLE facts (e.g. 0.7)
     */
    public FactVerifier(ModelClient model, int maxFactsToVerify,
                        double outdatedPenalty, double unverifiablePenalty) {
        this.model = model;
        this.maxFactsToVerify = maxFactsToVerify;
        this.outdatedPenalty = outdatedPenalty;
        this.unverifiablePenalty = unverifiablePenalty;
    }

    /** No-op verifier — passes everything through. */
    public static FactVerifier passThrough() {
        return new FactVerifier(null, 0, 1.0, 1.0);
    }

    /**
     * Verify a ranked list of recalled items against the given context.
     * Returns a (possibly shorter) list with confidence adjusted.
     *
     * @param items    ranked recall results
     * @param context  currently known context (recent messages, file contents, etc.)
     * @param minConf  confidence floor — items demoted below this are dropped
     */
    public List<MemoryItem> verify(List<MemoryItem> items, String context, double minConf) {
        if (model == null || items.isEmpty() || context == null || context.isBlank()) {
            return items; // pass-through
        }
        List<MemoryItem> out = new ArrayList<>(items.size());
        int verified = 0;
        for (MemoryItem item : items) {
            if (verified >= maxFactsToVerify) {
                // Past the cap — pass remaining through unverified.
                out.add(item);
                continue;
            }
            verified++;
            Verdict v = check(item.content(), context);
            double newConf = switch (v) {
                case CONFIRMED     -> item.confidence();
                case OUTDATED      -> item.confidence() * outdatedPenalty;
                case UNVERIFIABLE  -> item.confidence() * unverifiablePenalty;
            };
            if (newConf < minConf) {
                log.debug("Fact dropped ({}): {}", v, truncate(item.content()));
                continue;
            }
            if (newConf != item.confidence()) {
                out.add(withConfidence(item, newConf));
            } else {
                out.add(item);
            }
        }
        return out;
    }

    private Verdict check(String fact, String context) {
        try {
            String prompt = VERIFY_PROMPT.formatted(
                    truncate(context, 2000), truncate(fact, 500));
            String raw = model.complete(
                    ModelRequest.of(List.of(Message.user(prompt)))).text();
            if (raw == null) return Verdict.UNVERIFIABLE;
            String first = raw.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
            return switch (first) {
                case "CONFIRMED"    -> Verdict.CONFIRMED;
                case "OUTDATED"     -> Verdict.OUTDATED;
                default             -> Verdict.UNVERIFIABLE;
            };
        } catch (RuntimeException e) {
            log.debug("Verification failed: {}", e.toString());
            return Verdict.UNVERIFIABLE;
        }
    }

    private static MemoryItem withConfidence(MemoryItem m, double c) {
        return new MemoryItem(m.id(), m.tier(), m.content(), m.embedding(),
                m.tags(), m.source(), c,
                m.createdAt(), m.lastAccessedAt(), m.accessCount(), m.tokens());
    }

    private static String truncate(String s) { return truncate(s, 80); }
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public enum Verdict { CONFIRMED, OUTDATED, UNVERIFIABLE }
}
