package ai.mazehunt.api.agent;

/** Entry point to the platform — the user-facing orchestrator. */
public interface Agent {
    AgentResponse handle(AgentRequest request);
}
