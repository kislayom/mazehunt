package ai.mazehunt.core.boot;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.proactive.ProactiveEngine;
import ai.mazehunt.core.agent.MazehuntAgent;
import ai.mazehunt.core.config.MazehuntConfig;
import ai.mazehunt.core.memory.InMemoryVectorIndex;
import ai.mazehunt.core.memory.LayeredMemoryService;
import ai.mazehunt.core.memory.SqliteMemoryStore;
import ai.mazehunt.core.proactive.CuriosityBehaviour;
import ai.mazehunt.core.proactive.DefaultProactiveEngine;
import ai.mazehunt.core.proactive.MemoryConsolidationBehaviour;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.core.skill.SkillRegistry;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Boot-time wiring. Host modules (CLI / server / tests) register their model
 * clients with {@link #router()} and then call {@link #build(Function)} to get
 * a ready-to-use {@link MazehuntAgent}.
 */
public final class Mazehunt {

    private final MazehuntConfig config;
    private final SkillRegistry skills;
    private final ModelRouter router;

    private MemoryService memory;
    private DefaultProactiveEngine proactive;

    public Mazehunt(MazehuntConfig config) {
        this.config = config;
        this.skills = SkillRegistry.discoverFromServiceLoader();
        this.router = new ModelRouter(
                config.router().preferLocal(),
                config.router().cloudFallbackMinContext(),
                config.router().cloudFallbackComplexity());
    }

    public MazehuntConfig config()   { return config; }
    public SkillRegistry skills()    { return skills; }
    public ModelRouter router()      { return router; }
    public MemoryService memory()    { return memory; }
    public DefaultProactiveEngine proactive() { return proactive; }

    /**
     * Finalise wiring. Must be called <em>after</em> model clients are registered
     * with the router so the memory layer can pick up its embedder/summariser.
     */
    public MazehuntAgent build(Function<String, String> askUser) {
        ModelClient fast = router.pick(ModelRouter.Role.FAST, ModelRouter.Task.light());
        ModelClient embedModel = router.pick(ModelRouter.Role.EMBEDDING, ModelRouter.Task.light());
        EmbeddingClient embedder = embedModel instanceof EmbeddingClient ec ? ec : null;

        SqliteMemoryStore store = new SqliteMemoryStore(Path.of(config.memory().sqlitePath()));
        InMemoryVectorIndex index = new InMemoryVectorIndex();
        this.memory = new LayeredMemoryService(store, index, embedder, fast,
                config.memory().consolidateEveryNTurns(),
                config.memory().confidenceFloor());

        this.proactive = new DefaultProactiveEngine(memory, fast,
                config.proactive().maxNudgesPerHour());
        if (config.proactive().enabled()) {
            proactive.register(new MemoryConsolidationBehaviour());
            proactive.register(new CuriosityBehaviour());
            proactive.start();
        }

        return new MazehuntAgent(memory, skills, router, askUser,
                config.memory().workingTokenBudget());
    }

    /** Convenience accessor so hosts can cast if they want the concrete type. */
    public ProactiveEngine proactiveEngine() { return proactive; }
}
