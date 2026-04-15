package ai.mazehunt.core.boot;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.proactive.ProactiveEngine;
import ai.mazehunt.core.agent.MazehuntAgent;
import ai.mazehunt.core.config.MazehuntConfig;
import ai.mazehunt.core.proactive.CuriosityBehaviour;
import ai.mazehunt.core.proactive.DefaultProactiveEngine;
import ai.mazehunt.core.proactive.MemoryConsolidationBehaviour;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.core.skill.SkillRegistry;

import java.util.function.Function;

/**
 * Boot-time wiring. The bootstrap no longer owns the memory implementation —
 * callers inject a {@link MemoryService}, which is either an in-process
 * {@code LayeredMemoryService} (via {@code MemoryFactory.local}) or an HTTP
 * client to a stand-alone memory service (via {@code MemoryFactory.remote}).
 *
 * <p>Typical host wiring:
 * <pre>{@code
 *   Mazehunt mh = new Mazehunt(cfg);
 *   ModelFactory.registerAll(cfg, mh.router());
 *   MemoryService mem = useRemote
 *       ? MemoryFactory.remote(cfg.memory().remoteEndpoint())
 *       : MemoryFactory.local(...);
 *   MazehuntAgent agent = mh.build(mem, askUser);
 * }</pre>
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

    public MazehuntConfig config()            { return config; }
    public SkillRegistry skills()             { return skills; }
    public ModelRouter router()               { return router; }
    public MemoryService memory()             { return memory; }
    public DefaultProactiveEngine proactive() { return proactive; }
    public ProactiveEngine proactiveEngine()  { return proactive; }

    /**
     * Finalise wiring given an externally-constructed memory service. The
     * router must already have its models registered.
     */
    public MazehuntAgent build(MemoryService memory, Function<String, String> askUser) {
        this.memory = memory;
        ModelClient fast = router.pick(ModelRouter.Role.FAST, ModelRouter.Task.light());

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
}
