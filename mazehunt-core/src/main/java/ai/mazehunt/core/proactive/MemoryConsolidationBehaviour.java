package ai.mazehunt.core.proactive;

import ai.mazehunt.api.proactive.ProactiveBehaviour;
import ai.mazehunt.api.proactive.ProactiveContext;

import java.time.Duration;

/** Periodically distils episodic memory into semantic facts. */
public final class MemoryConsolidationBehaviour implements ProactiveBehaviour {
    @Override public String name() { return "memory.consolidation"; }
    @Override public Duration interval() { return Duration.ofMinutes(30); }
    @Override public void tick(ProactiveContext ctx) { ctx.memory().consolidate(); }
}
