package ai.mazehunt.cli;

import ai.mazehunt.api.agent.AgentRequest;
import ai.mazehunt.api.agent.AgentResponse;
import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.core.agent.MazehuntAgent;
import ai.mazehunt.core.boot.Mazehunt;
import ai.mazehunt.core.config.ConfigLoader;
import ai.mazehunt.core.config.MazehuntConfig;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.memory.MemoryFactory;
import ai.mazehunt.models.ModelFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Interactive REPL. Supports a handful of slash commands:
 * <pre>
 *   /help           show commands
 *   /memory stats   memory tier counts
 *   /nudges         drain proactive nudges
 *   /config         print active config
 *   /quit
 * </pre>
 * Everything else is treated as a user goal for the agent.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        Path cfgPath = args.length > 0 ? Path.of(args[0]) : Path.of("config/mazehunt.yaml");
        MazehuntConfig cfg = ConfigLoader.loadOrDefault(cfgPath);

        Mazehunt mh = new Mazehunt(cfg);
        ModelFactory.registerAll(cfg, mh.router());

        MemoryService memory = resolveMemory(cfg, mh.router());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        MazehuntAgent agent = mh.build(memory, prompt -> {
            System.out.print("\n[agent asks] " + prompt + "\n> ");
            try { return in.readLine(); } catch (Exception e) { return ""; }
        });

        String sessionId = UUID.randomUUID().toString();
        System.out.println("Mazehunt ready. Session " + sessionId + ". Type /help.");

        while (true) {
            drainNudges(mh);
            System.out.print("\n> ");
            String line = in.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("/")) {
                if (!handleCommand(line, mh)) break;
                continue;
            }
            AgentResponse resp = agent.handle(new AgentRequest(sessionId, List.of(), line));
            System.out.println("\n" + resp.answer());
            if (!resp.openQuestions().isEmpty()) {
                System.out.println("\n-- open questions --");
                resp.openQuestions().forEach(q -> System.out.println("  • " + q));
            }
        }
        mh.proactive().stop();
    }

    /**
     * Pick between local in-process memory and an HTTP client to a stand-alone
     * memory service, based on {@code memory.mode} in the config.
     */
    private static MemoryService resolveMemory(MazehuntConfig cfg, ModelRouter router) {
        if (cfg.memory().isRemote()) {
            String url = cfg.memory().remoteEndpoint();
            System.out.println("Memory: remote @ " + url);
            return MemoryFactory.remote(url);
        }
        ModelClient fast = router.pick(ModelRouter.Role.FAST, ModelRouter.Task.light());
        ModelClient embedModel = router.pick(ModelRouter.Role.EMBEDDING, ModelRouter.Task.light());
        EmbeddingClient embedder = embedModel instanceof EmbeddingClient ec ? ec : null;
        System.out.println("Memory: local @ " + cfg.memory().sqlitePath());
        return MemoryFactory.local(Path.of(cfg.memory().sqlitePath()),
                embedder, fast,
                cfg.memory().consolidateEveryNTurns(),
                cfg.memory().confidenceFloor());
    }

    private static boolean handleCommand(String line, Mazehunt mh) {
        switch (line) {
            case "/help" -> System.out.println("""
                    /help           this message
                    /memory stats   memory tier counts
                    /memory recall <query>  quick recall peek
                    /nudges         drain proactive nudges
                    /config         print active config summary
                    /quit           exit
                    """);
            case "/quit" -> { return false; }
            case "/memory stats" -> mh.memory().stats().forEach(
                    (t, c) -> System.out.printf("  %-10s %d%n", t, c));
            case "/nudges"  -> drainNudges(mh);
            case "/config"  -> {
                var c = mh.config();
                System.out.println("profile=" + c.hardware().profile()
                        + " gpu=" + c.hardware().gpuVramGb() + "GB"
                        + " ram=" + c.hardware().systemRamGb() + "GB");
                c.models().forEach(m -> System.out.println("  model " + m.role()
                        + " " + m.id() + " via " + m.provider() + (m.local() ? " (local)" : " (cloud)")));
            }
            default -> {
                if (line.startsWith("/memory recall ")) {
                    String q = line.substring("/memory recall ".length());
                    List<MemoryItem> hits = mh.memory().recall(
                            ai.mazehunt.api.memory.RecallRequest.of(q, 800));
                    if (hits.isEmpty()) System.out.println("(nothing)");
                    for (MemoryItem m : hits) {
                        System.out.printf("  [%s, %.2f] %s%n",
                                m.tier(), m.confidence(), m.content());
                    }
                } else {
                    System.out.println("unknown command; try /help");
                }
            }
        }
        return true;
    }

    private static void drainNudges(Mazehunt mh) {
        if (mh.proactive() == null) return;
        for (String n : mh.proactive().drainNudges()) {
            System.out.println("\n[proactive] " + n);
        }
    }
}
