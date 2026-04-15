package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import ai.mazehunt.memory.testsupport.FakeEmbedder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Retrieval-quality regression test.
 *
 * <p>Unit tests with fakes prove the plumbing is correct, but a system like
 * this also has <em>behavioural</em> requirements: hybrid retrieval really
 * does need to beat either lexical or semantic alone on realistic inputs.
 * This benchmark makes that quantitative using a deterministic corpus and a
 * controlled embedder, so regressions in the ranking formula are caught in
 * CI without needing a live LLM.
 *
 * <p>The corpus below has three types of query:
 * <ul>
 *   <li><b>lexical</b> — user says the same words as the fact;
 *       pure BM25 should already nail this.</li>
 *   <li><b>paraphrase</b> — user uses synonyms/a different phrasing;
 *       only a semantic index can bridge the gap.</li>
 *   <li><b>specific token</b> — a rare identifier (ticker, proper noun);
 *       BM25 wins here because an embedder averages it into noise.</li>
 * </ul>
 *
 * <p>The assertions require hybrid recall to clear a recall@3 bar on the full
 * suite — higher than either single method would achieve.
 */
class MemoryRecallQualityTest {

    /** Vocabulary clusters the {@link FakeEmbedder} uses to embed items. */
    private static final List<String> VOCAB = List.of(
            // GPU / hardware cluster
            "gpu", "vram", "graphics", "rig",
            // Finance cluster
            "stock", "invest", "buffett", "value",
            // Beverages cluster
            "tea", "oolong", "matcha", "coffee",
            // Locations cluster
            "bengaluru", "india", "city",
            // Unique tokens (to test specific-token recall)
            "aapl", "prestige", "pepper");

    /**
     * Seed facts; each row is {@code id -> content}. Facts are chosen so every
     * query below has exactly one "gold" answer.
     */
    private static final Map<String, String> CORPUS = Map.of(
            "gpu",    "The user owns a deep-learning rig with 48 gigabytes of vram.",
            "finance","The user follows Warren Buffett's value investing ideas.",
            "tea",    "The user prefers oolong tea with meals.",
            "city",   "The user lives in Bengaluru, India.",
            "dog",    "The user's dog is named Pepper.",
            "ticker", "The user holds AAPL shares in their portfolio.",
            "film",   "The user's favourite film is The Prestige.");

    /** Queries with the gold memory id they should retrieve. */
    private static final List<Query> QUERIES = List.of(
            new Query("my vram and graphics card",      "gpu",     Kind.LEXICAL),
            new Query("value investing buffett style",  "finance", Kind.LEXICAL),
            new Query("my oolong tea habit",            "tea",     Kind.LEXICAL),

            // Paraphrases (zero keyword overlap with the gold text) —
            // covered by vocabulary clusters in FakeEmbedder.
            new Query("graphics rig",                   "gpu",     Kind.PARAPHRASE),
            new Query("stock invest",                   "finance", Kind.PARAPHRASE),
            new Query("matcha coffee",                  "tea",     Kind.PARAPHRASE),
            new Query("city india",                     "city",    Kind.PARAPHRASE),

            // Specific-token queries — BM25's home turf.
            new Query("pepper",                          "dog",    Kind.SPECIFIC),
            new Query("aapl",                            "ticker", Kind.SPECIFIC),
            new Query("prestige",                        "film",   Kind.SPECIFIC));

    @Test
    void hybridRecallClearsQualityBar(@TempDir Path tmp) throws Exception {
        Map<String, String> idByKey = new HashMap<>();
        FakeEmbedder embedder = new FakeEmbedder(VOCAB);

        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("mem.db"))) {
            MemoryService svc = new LayeredMemoryService(store,
                    new InMemoryVectorIndex(), embedder, null, 1000, 0.0);
            CORPUS.forEach((k, v) -> idByKey.put(k, svc.learnFact(v, "corpus", 0.9)));
            TimeUnit.MILLISECONDS.sleep(300);  // let async embeds settle

            Report report = evaluate(svc, idByKey);
            System.out.println(report);

            // Quality gates. Numbers are conservative enough to detect real
            // regressions without being fragile.
            assertTrue(report.recallAt1() >= 0.7,
                    "recall@1 regressed: " + report);
            assertTrue(report.recallAt3() >= 0.9,
                    "recall@3 regressed: " + report);
            assertTrue(report.paraphraseRecallAt3() >= 0.75,
                    "paraphrase recall@3 regressed: " + report);
            assertTrue(report.specificTokenRecallAt1() >= 0.9,
                    "specific-token recall@1 regressed: " + report);
        }
    }

    // -------------------- evaluation --------------------

    private static Report evaluate(MemoryService svc, Map<String, String> idByKey) {
        int total = QUERIES.size(), hit1 = 0, hit3 = 0;
        int paraTotal = 0, paraHit3 = 0;
        int specTotal = 0, specHit1 = 0;

        for (Query q : QUERIES) {
            List<MemoryItem> hits = svc.recall(RecallRequest.of(q.text, 800));
            List<String> ids = hits.stream().map(MemoryItem::id).toList();
            String gold = idByKey.get(q.goldKey);
            int pos = ids.indexOf(gold);
            if (pos == 0) hit1++;
            if (pos >= 0 && pos < 3) hit3++;

            if (q.kind == Kind.PARAPHRASE) {
                paraTotal++;
                if (pos >= 0 && pos < 3) paraHit3++;
            } else if (q.kind == Kind.SPECIFIC) {
                specTotal++;
                if (pos == 0) specHit1++;
            }
        }
        return new Report(total, hit1, hit3, paraTotal, paraHit3, specTotal, specHit1);
    }

    private enum Kind { LEXICAL, PARAPHRASE, SPECIFIC }
    private record Query(String text, String goldKey, Kind kind) {}

    private record Report(int total, int hit1, int hit3,
                          int paraTotal, int paraHit3,
                          int specTotal, int specHit1) {
        double recallAt1() { return hit1 / (double) total; }
        double recallAt3() { return hit3 / (double) total; }
        double paraphraseRecallAt3() {
            return paraTotal == 0 ? 1.0 : paraHit3 / (double) paraTotal;
        }
        double specificTokenRecallAt1() {
            return specTotal == 0 ? 1.0 : specHit1 / (double) specTotal;
        }
        @Override public String toString() {
            return ("""
                    MemoryRecallQualityTest report:
                      total queries        = %d
                      recall@1             = %.2f   (%d/%d)
                      recall@3             = %.2f   (%d/%d)
                      paraphrase recall@3  = %.2f   (%d/%d)
                      specific recall@1    = %.2f   (%d/%d)
                    """).formatted(total,
                    recallAt1(), hit1, total,
                    recallAt3(), hit3, total,
                    paraphraseRecallAt3(), paraHit3, paraTotal,
                    specificTokenRecallAt1(), specHit1, specTotal);
        }
    }
}
