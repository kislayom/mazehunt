package ai.mazehunt.api.agent;

import ai.mazehunt.api.plan.Plan;
import ai.mazehunt.api.plan.StepResult;
import java.util.List;

/** Agent's answer to an {@link AgentRequest}. */
public record AgentResponse(
        String answer,
        Plan plan,
        List<StepResult> stepResults,
        /** Questions the agent wants the user to answer before it can be confident. */
        List<String> openQuestions,
        /** Grounded claims used to build {@link #answer}. */
        List<String> citations
) {}
