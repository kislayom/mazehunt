package ai.mazehunt.api.agent;

import ai.mazehunt.api.model.Message;
import java.util.List;

/** A top-level user request to the agent. */
public record AgentRequest(String sessionId, List<Message> conversation, String userGoal) {}
