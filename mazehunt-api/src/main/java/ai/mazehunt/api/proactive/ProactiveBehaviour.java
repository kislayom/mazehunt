package ai.mazehunt.api.proactive;

import java.time.Duration;

/**
 * A standing rule the agent applies to itself — e.g. "once a day, look for
 * conflicts in the user's knowledge graph and ask clarifying questions".
 *
 * <p>Contract: {@link #tick(ProactiveContext)} must be idempotent and cheap;
 * heavyweight work should be scheduled via
 * {@link ProactiveEngine#schedule(String, Duration, Runnable)}.
 */
public interface ProactiveBehaviour {
    String name();
    Duration interval();
    void tick(ProactiveContext ctx);
}
