package ai.mazehunt.core.agent;

import ai.mazehunt.api.agent.Agent;
import ai.mazehunt.api.agent.AgentRequest;
import ai.mazehunt.api.agent.AgentResponse;
import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.api.plan.Plan;
import ai.mazehunt.api.plan.StepResult;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.core.plan.PlanExecutor;
import ai.mazehunt.core.plan.Planner;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.core.skill.DefaultSkillContext;
import ai.mazehunt.core.skill.SkillRegistry;
import ai.mazehunt.core.util.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Front-door orchestrator: plans, executes, synthesises the final answer while
 * enforcing the grounding contract. The key trick for context minimisation is
 * {@link #buildWorkingContext}: we hand the model a compact bundle of
 * <em>retrieved</em> facts instead of the full conversation.
 */
public final class MazehuntAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MazehuntAgent.class);

    private final MemoryService memory;
    private final SkillRegistry skills;
    private final ModelRouter router;
    private final Planner planner;
    private final Function<String, String> askUser;
    private final int workingTokenBudget;

    public MazehuntAgent(MemoryService memory, SkillRegistry skills, ModelRouter router,
                         Function<String, String> askUser, int workingTokenBudget) {
        this.memory = memory;
        this.skills = skills;
        this.router = router;
        this.askUser = askUser;
        this.workingTokenBudget = workingTokenBudget;
        this.planner = new Planner(router.pick(ModelRouter.Role.REASONING,
                                               ModelRouter.Task.light()), skills);
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        log.info("Handling goal: {}", request.userGoal());

        // 1. Store user utterance as episodic memory.
        memory.remember("USER: " + request.userGoal(),
                Map.of("session", request.sessionId(), "role", "user"),
                "conversation");

        // 2. Build a compact working context from recall + recent turns.
        String workingContext = buildWorkingContext(request);

        // 3. Plan.
        Plan plan = planner.plan(request.userGoal(), workingContext);
        log.debug("Plan:\n{}", plan.toMermaid());

        // 4. Execute DAG.
        ModelClient executorModel = router.pick(ModelRouter.Role.REASONING,
                new ModelRouter.Task(
                        Tokens.count(workingContext) + plan.steps().size() * 200,
                        plan.steps().size(),
                        ai.mazehunt.api.Modality.TEXT));
        SkillContext ctx = new DefaultSkillContext(memory, executorModel);
        PlanExecutor exec = new PlanExecutor(memory, skills, executorModel, askUser);
        List<StepResult> results = exec.execute(plan, ctx);

        // 5. Collect open questions (UNKNOWN steps) and citations.
        List<String> openQuestions = new ArrayList<>();
        List<String> citations = new ArrayList<>();
        for (StepResult r : results) {
            if (r.status() == StepResult.Status.UNKNOWN) {
                openQuestions.add("Missing info for step '" + r.stepId() + "': " + r.error());
            }
            if (r.citations() != null) citations.addAll(r.citations());
        }

        // 6. Synthesise the final answer grounded in results.
        String answer = synthesise(request.userGoal(), results, openQuestions, executorModel);

        // 7. Store assistant turn.
        memory.remember("ASSISTANT: " + answer,
                Map.of("session", request.sessionId(), "role", "assistant"),
                "conversation");

        return new AgentResponse(answer, plan, results, openQuestions, citations);
    }

    /**
     * Assemble exactly the material the model needs — and nothing else. This is
     * what keeps the live prompt small while still letting the model "remember
     * everything": the facts are selected by the memory service, not by simply
     * replaying the conversation.
     */
    private String buildWorkingContext(AgentRequest request) {
        int budget = Math.max(500, workingTokenBudget);
        int recallBudget = Math.min(budget - 400, 1200);
        List<MemoryItem> recalled = memory.recall(RecallRequest.of(request.userGoal(), recallBudget));
        StringBuilder sb = new StringBuilder();
        sb.append("## Relevant facts (with sources)\n");
        if (recalled.isEmpty()) sb.append("(none on file)\n");
        for (MemoryItem m : recalled) {
            sb.append("- [").append(m.tier()).append(", conf=")
              .append(String.format("%.2f", m.confidence())).append(", src=")
              .append(m.source() == null ? "?" : m.source()).append("] ")
              .append(m.content()).append('\n');
        }
        // Tail of the conversation for coherence — capped hard.
        int convBudget = 400;
        StringBuilder tail = new StringBuilder();
        if (request.conversation() != null) {
            List<Message> msgs = request.conversation();
            for (int i = msgs.size() - 1; i >= 0; i--) {
                String piece = msgs.get(i).role() + ": " + msgs.get(i).text() + "\n";
                if (Tokens.count(piece) > convBudget) break;
                tail.insert(0, piece);
                convBudget -= Tokens.count(piece);
            }
        }
        if (tail.length() > 0) sb.append("\n## Recent turns\n").append(tail);
        return sb.toString();
    }

    private String synthesise(String goal, List<StepResult> results,
                              List<String> openQuestions, ModelClient model) {
        StringBuilder res = new StringBuilder();
        for (StepResult r : results) {
            res.append("- step ").append(r.stepId()).append(" [").append(r.status()).append("]: ");
            if (r.status() == StepResult.Status.OK) res.append(String.valueOf(r.output()));
            else res.append(r.error() == null ? "" : r.error());
            res.append('\n');
        }
        String sys = """
                You are the final-answer synthesiser. You MUST:
                - only state facts that appear in the step outputs or recalled memory;
                - if a fact is missing, say so plainly and list the open questions;
                - be concise, direct, and human — like a trusted assistant, not a chatbot;
                - never invent numbers, names, or dates.
                """;
        String user = """
                Goal: %s
                Step outputs:
                %s
                Open questions (if any): %s
                Write the answer now.
                """.formatted(goal, res, openQuestions);
        return model.complete(ModelRequest.of(List.of(
                Message.system(sys), Message.user(user)))).text();
    }
}
