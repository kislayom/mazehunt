package ai.mazehunt.api.proactive;

import java.time.Duration;

/**
 * Fires periodic self-directed actions: consolidating memory, asking the user
 * follow-up questions, running background research, etc. This is what makes the
 * platform "assistant-like" rather than a reactive chatbot.
 */
public interface ProactiveEngine {

    void start();
    void stop();

    /** Hook used by skills to request a one-off future action. */
    void schedule(String name, Duration delay, Runnable action);

    /** Register an ongoing curiosity / check-in behaviour. */
    void register(ProactiveBehaviour behaviour);
}
