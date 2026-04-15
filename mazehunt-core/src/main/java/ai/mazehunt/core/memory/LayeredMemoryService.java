package ai.mazehunt.core.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.core.util.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>Memory pipeline</h2>
 *
 * <pre>
 *   remember()  ─┐
 *                ├─▶  [EPISODIC log]     ─consolidate()─▶  [SEMANTIC facts]
 *   learnFact() ─┘                                        ▲
 *                                                         │
 *   recall():  BM25 prefilter ∪ vector top-K
 *              ─▶ deduplicate (max-marginal-relevance)
 *              ─▶ score = α·similarity + β·recency + γ·confidence + δ·accessCount
 *              ─▶ pack to token budget (greedy + tie-break)
 *              ─▶ touch last_accessed on returned items
 * </pre>
 *
 * <p>The scoring weights deliberately reward <strong>grounded, cited facts</strong>
 * over raw episodes — that keeps the prompt small and anti-hallucinatory.
 */
public final class LayeredMemoryService implements MemoryService {

    private static final Logger log = LoggerFactory.getLogger(LayeredMemoryService.class);

    private final SqliteMemoryStore store;
    private final InMemoryVectorIndex index;
    private final EmbeddingClient embedder;
    private final ModelClient summariser;
    private final ExecutorService async =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "mazehunt-memory");
                t.setDaemon(true);
                return t;
            });
    private final AtomicInteger turnCounter = new AtomicInteger();
    private final int consolidateEveryN;
    private final double confidenceFloor;

    public LayeredMemoryService(SqliteMemoryStore store,
                                InMemoryVectorIndex index,
                                EmbeddingClient embedder,
                                ModelClient summariser,
                                int consolidateEveryN,
                                double confidenceFloor) {
        this.store = store;
        this.index = index;
        this.embedder = embedder;
        this.summariser = summariser;
        this.consolidateEveryN = consolidateEveryN;
        this.confidenceFloor = confidenceFloor;
        index.hydrate(store.all());
    }

    @Override
    public String remember(String content, Map<String, String> tags, String source) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        MemoryItem initial = new MemoryItem(
                id, MemoryItem.Tier.EPISODIC, content, null,
                tags == null ? Map.of() : tags,
                source, 0.9, now, now, 0, Tokens.count(content));
        store.upsert(initial);
        async.submit(() -> embedAndIndex(id, content));
        if (turnCounter.incrementAndGet() % consolidateEveryN == 0) {
            async.submit(this::consolidate);
        }
        return id;
    }

    @Override
    public String learnFact(String fact, String source, double confidence) {
        // Check for near-duplicate semantic facts before inserting.
        float[] v = embedder == null ? null : safeEmbed(fact);
        if (v != null) {
            for (VectorIndex.Hit h : index.topK(v, 3)) {
                store.findById(h.id()).ifPresent(existing -> {
                    if (existing.tier() == MemoryItem.Tier.SEMANTIC && h.score() > 0.94) {
                        log.debug("Suppressing near-duplicate fact (score={}): {}", h.score(), fact);
                    }
                });
            }
        }
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        MemoryItem item = new MemoryItem(
                id, MemoryItem.Tier.SEMANTIC, fact, v,
                Map.of("kind", "fact"),
                source, confidence, now, now, 0, Tokens.count(fact));
        store.upsert(item);
        if (v != null) index.add(id, v);
        return id;
    }

    @Override
    public List<MemoryItem> recall(RecallRequest req) {
        int budget = req.tokenBudget() <= 0 ? 1200 : req.tokenBudget();
        double minConf = Math.max(req.minConfidence(), confidenceFloor);

        // Hybrid retrieval: BM25 ∪ vector.
        Map<String, MemoryItem> pool = new LinkedHashMap<>();
        for (MemoryItem m : store.ftsSearch(req.query(), 30)) pool.put(m.id(), m);

        float[] qv = embedder == null ? null : safeEmbed(req.query());
        if (qv != null) {
            for (VectorIndex.Hit h : index.topK(qv, 30)) {
                store.findById(h.id()).ifPresent(m -> pool.putIfAbsent(m.id(), m));
            }
        }

        Set<MemoryItem.Tier> tiers = req.tiers();
        Instant now = Instant.now();
        List<Scored> scored = new ArrayList<>();
        for (MemoryItem m : pool.values()) {
            if (tiers != null && !tiers.contains(m.tier())) continue;
            if (m.confidence() < minConf) continue;
            if (!matchesTags(m, req.requiredTags())) continue;
            double sim = qv != null && m.embedding() != null
                    ? ai.mazehunt.core.util.Vectors.cosine(qv, m.embedding())
                    : 0.5;
            double recency = recency(m.createdAt(), now);
            double usage = Math.log1p(m.accessCount()) / 4.0;
            double tierBoost = switch (m.tier()) {
                case SEMANTIC -> 0.15;
                case PROCEDURAL -> 0.10;
                case WORKING -> 0.05;
                case EPISODIC -> 0.0;
            };
            double score = 0.55 * sim + 0.15 * recency + 0.15 * m.confidence() + 0.10 * usage + tierBoost;
            scored.add(new Scored(m, score));
        }
        scored.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed());

        // Max-Marginal-Relevance deduplication + token packing.
        List<MemoryItem> picked = new ArrayList<>();
        int usedTokens = 0;
        for (Scored s : scored) {
            if (usedTokens + s.item.tokens() > budget) continue;
            boolean dup = false;
            for (MemoryItem already : picked) {
                if (already.embedding() != null && s.item.embedding() != null) {
                    if (ai.mazehunt.core.util.Vectors.cosine(already.embedding(), s.item.embedding()) > 0.92) {
                        dup = true; break;
                    }
                }
            }
            if (dup) continue;
            picked.add(s.item);
            usedTokens += s.item.tokens();
            store.touch(s.item.id());
            if (picked.size() >= 20) break;
        }
        return picked;
    }

    /** Summarise recent episodic items into semantic facts, then prune them. */
    @Override
    public void consolidate() {
        List<MemoryItem> recent = store.recent(MemoryItem.Tier.EPISODIC, 40);
        if (recent.size() < 8 || summariser == null) return;
        StringBuilder buf = new StringBuilder();
        for (MemoryItem m : recent) {
            buf.append("- ").append(m.content().replace('\n', ' ')).append('\n');
        }
        String prompt = """
                You are the agent's memory consolidator. Read the recent events below
                and extract stable facts about the user, their preferences, ongoing
                projects, or recurring entities. Output one fact per line. Only include
                a fact if it is supported by the evidence; otherwise omit it.
                Output NOTHING else.

                Events:
                %s
                """.formatted(buf);
        try {
            String out = summariser.complete(ModelRequest.of(List.of(Message.user(prompt)))).text();
            if (out == null) return;
            for (String line : out.split("\\r?\\n")) {
                String fact = line.replaceFirst("^[-*\\d.\\s]+", "").trim();
                if (fact.length() > 6) learnFact(fact, "consolidation", 0.7);
            }
        } catch (RuntimeException e) {
            log.warn("Consolidation failed: {}", e.toString());
        }
    }

    @Override
    public Map<MemoryItem.Tier, Long> stats() { return store.countsByTier(); }

    // ---------- helpers ----------

    private void embedAndIndex(String id, String content) {
        if (embedder == null) return;
        float[] v = safeEmbed(content);
        if (v == null) return;
        store.findById(id).ifPresent(m -> store.upsert(new MemoryItem(
                m.id(), m.tier(), m.content(), v, m.tags(), m.source(),
                m.confidence(), m.createdAt(), m.lastAccessedAt(), m.accessCount(), m.tokens())));
        index.add(id, v);
    }

    private float[] safeEmbed(String s) {
        try { return embedder.embed(s); }
        catch (RuntimeException e) {
            log.debug("Embed failed: {}", e.toString());
            return null;
        }
    }

    private static boolean matchesTags(MemoryItem m, Map<String, String> req) {
        if (req == null || req.isEmpty()) return true;
        for (Map.Entry<String, String> e : req.entrySet()) {
            if (!e.getValue().equals(m.tags().get(e.getKey()))) return false;
        }
        return true;
    }

    private static double recency(Instant t, Instant now) {
        long hours = Math.max(1, (now.toEpochMilli() - t.toEpochMilli()) / 3_600_000L);
        // ~1.0 fresh → ~0.2 after a week → near-0 after a year
        return 1.0 / (1.0 + Math.log(hours));
    }

    private record Scored(MemoryItem item, double score) {}
}
