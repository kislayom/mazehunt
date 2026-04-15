package ai.mazehunt.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import ai.mazehunt.models.ollama.OllamaClient;
import ai.mazehunt.models.ollama.OllamaEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test against a real local Ollama server. Skipped by default so CI
 * stays green on hosts without a GPU — enable by setting:
 *
 * <pre>
 *   export OLLAMA_URL=http://localhost:11434
 *   # optional overrides:
 *   export OLLAMA_EMBED_MODEL=nomic-embed-text
 *   export OLLAMA_CHAT_MODEL=gemma2:9b
 *   mvn -pl mazehunt-memory test -Dtest=LiveOllamaMemoryIT
 * </pre>
 *
 * <p>What this proves that the unit tests can't:
 * <ul>
 *   <li>Real embeddings actually place paraphrases near each other.</li>
 *   <li>Hybrid recall surfaces the right fact when the query has zero
 *       keyword overlap (BM25 alone would miss it).</li>
 *   <li>Consolidation produces grounded, quotable facts from a real model.</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "OLLAMA_URL",
        matches = ".+",
        disabledReason = "set OLLAMA_URL=http://localhost:11434 to run")
class LiveOllamaMemoryTest {

    private static String endpoint()    { return System.getenv("OLLAMA_URL"); }
    private static String embedModel()  {
        return System.getenv().getOrDefault("OLLAMA_EMBED_MODEL", "nomic-embed-text");
    }
    private static String chatModel()   {
        return System.getenv().getOrDefault("OLLAMA_CHAT_MODEL", "gemma2:9b");
    }

    @Test
    void paraphraseRecallBeatsKeywordOnly(@TempDir Path tmp) throws Exception {
        OllamaEmbeddingClient embedder = new OllamaEmbeddingClient(endpoint(), embedModel());

        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("mem.db"))) {
            MemoryService svc = new LayeredMemoryService(store,
                    new InMemoryVectorIndex(), embedder, null, 100, 0.0);

            // Seed five facts with deliberately non-overlapping wording.
            String target = svc.learnFact(
                    "The user operates a high-end deep-learning rig with 48 gigabytes of graphics memory.",
                    "setup", 0.95);
            svc.learnFact("The user lives in Bengaluru and works in finance.",   "profile", 0.9);
            svc.learnFact("The user prefers matcha over oolong.",                "profile", 0.7);
            svc.learnFact("The user's dog is named Pepper.",                     "profile", 0.8);
            svc.learnFact("The user's favourite movie is The Prestige.",         "profile", 0.7);

            // Give the async embedder a moment to finish.
            waitForEmbeddings();

            // Query that shares zero lexical tokens with the target fact —
            // only a real embedding model should find it.
            List<MemoryItem> hits = svc.recall(
                    RecallRequest.of("how much VRAM does my machine have", 600));

            assertFalse(hits.isEmpty(), "expected at least one hit from live recall");
            assertEquals(target, hits.get(0).id(),
                    "semantic paraphrase should surface the GPU fact first; got: " +
                            hits.get(0).content());
        }
    }

    @Test
    void consolidationProducesGroundedFacts(@TempDir Path tmp) throws Exception {
        OllamaEmbeddingClient embedder = new OllamaEmbeddingClient(endpoint(), embedModel());
        OllamaClient chat = new OllamaClient(endpoint(), chatModel(), 8192, false, Map.of());

        try (SqliteMemoryStore store = new SqliteMemoryStore(tmp.resolve("mem.db"))) {
            MemoryService svc = new LayeredMemoryService(store,
                    new InMemoryVectorIndex(), embedder, chat, 100, 0.0);

            svc.remember("USER: I always prefer replies in bullet points.",         Map.of(), "chat");
            svc.remember("USER: My trading style follows Warren Buffett's ideas.",  Map.of(), "chat");
            svc.remember("USER: I like oolong tea after lunch every day.",          Map.of(), "chat");
            svc.remember("USER: My working hours are 9am to 6pm IST.",              Map.of(), "chat");
            svc.remember("USER: I keep my code in Java 21 with Maven.",             Map.of(), "chat");
            svc.remember("USER: I run local LLMs on a 48 GB VRAM workstation.",     Map.of(), "chat");
            svc.remember("USER: I usually skim papers on arXiv in the evening.",    Map.of(), "chat");
            svc.remember("USER: I dislike notifications during focus hours.",       Map.of(), "chat");
            waitForEmbeddings();

            long semanticBefore = svc.stats().get(MemoryItem.Tier.SEMANTIC);
            svc.consolidate();
            long semanticAfter  = svc.stats().get(MemoryItem.Tier.SEMANTIC);

            assertTrue(semanticAfter > semanticBefore,
                    "real consolidation should produce at least one semantic fact");
            // Spot-check: at least one of the extracted facts should mention something we told it.
            List<MemoryItem> facts = svc.recall(new RecallRequest(
                    "user preferences workstation", 600,
                    java.util.Set.of(MemoryItem.Tier.SEMANTIC), Map.of(), 0.0));
            boolean mentionsKnownDetail = facts.stream().anyMatch(m -> {
                String c = m.content().toLowerCase();
                return c.contains("java") || c.contains("buffett") || c.contains("oolong")
                        || c.contains("vram") || c.contains("48") || c.contains("bullet");
            });
            assertTrue(mentionsKnownDetail,
                    "consolidated facts should echo something the user said; got: " +
                            facts.stream().map(MemoryItem::content).toList());
        }
    }

    private static void waitForEmbeddings() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
    }
}
