package ai.mazehunt.core.plan;

import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.api.plan.Plan;
import ai.mazehunt.api.plan.PlanStep;
import ai.mazehunt.core.skill.SkillRegistry;
import ai.mazehunt.core.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Turns a free-form goal into a typed {@link Plan}. The planner prompts a
 * reasoning model to emit JSON describing steps; the result is validated and
 * coerced into our in-memory representation.
 *
 * <p>If the model returns malformed JSON we fall back to a single-step
 * "direct answer" plan rather than error out.
 */
public final class Planner {

    private static final Logger log = LoggerFactory.getLogger(Planner.class);

    private static final String SYSTEM_TEMPLATE = """
        You are a planning module inside an agentic AI system.
        Produce a JSON plan that breaks the user's goal into small, concrete steps.
        Each step has:
          - id: short snake_case identifier (unique within the plan)
          - description: one sentence, imperative
          - kind: one of TOOL | MODEL | RECALL | ASK_USER | MAP | REDUCE
          - target: the tool name for TOOL steps, or a hint for others ("" if unused)
          - inputs: object mapping parameter-name -> value-ref, where value-ref is
              {"literal": <json>}  or  {"step": "<id>", "path": "<jsonpath>"} or
              {"memory": "<query>"} or  {"ask": "<question>"}
          - depends_on: list of step ids this step reads from
          - condition: optional natural-language gating condition, or ""
        Rules:
          * Use RECALL before MODEL if facts are needed.
          * Use ASK_USER when information is genuinely missing and cannot be inferred.
          * Never fabricate facts — route uncertainty through RECALL or ASK_USER.
          * Prefer the fewest steps that will actually answer the goal.
          * Output STRICT JSON only: {"steps":[...],"root":"<last_step_id>"}.

        Available tools:
        %s
        """;

    private final ModelClient model;
    private final SkillRegistry skills;

    public Planner(ModelClient model, SkillRegistry skills) {
        this.model = model;
        this.skills = skills;
    }

    public Plan plan(String goal, String conversationContext) {
        String system = SYSTEM_TEMPLATE.formatted(skills.describeForPrompt());
        String user = """
                Conversation so far:
                %s

                Goal: %s
                """.formatted(conversationContext == null ? "(none)" : conversationContext, goal);
        try {
            String raw = model.complete(ModelRequest.of(List.of(
                    Message.system(system), Message.user(user)))).text();
            String json = extractJson(raw);
            return parse(goal, json);
        } catch (RuntimeException e) {
            log.warn("Planner fallback (reason: {})", e.toString());
            return fallback(goal);
        }
    }

    /** Trivial single-step plan used when planning itself fails. */
    public static Plan fallback(String goal) {
        PlanStep only = new PlanStep(
                "answer", "Answer the user directly using available context.",
                PlanStep.Kind.MODEL, "reasoning",
                Map.of("goal", new PlanStep.ValueRef.Literal(goal)),
                List.of(), "");
        return new Plan(goal, List.of(only), "answer");
    }

    private static String extractJson(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return "{}";
        return raw.substring(start, end + 1);
    }

    private static Plan parse(String goal, String json) {
        JsonNode root = Json.tree(json);
        JsonNode stepsNode = root.path("steps");
        List<PlanStep> steps = new ArrayList<>();
        for (JsonNode s : stepsNode) {
            String id = s.path("id").asText(UUID.randomUUID().toString().substring(0, 8));
            String description = s.path("description").asText("");
            String kindStr = s.path("kind").asText("MODEL").toUpperCase(Locale.ROOT);
            PlanStep.Kind kind;
            try { kind = PlanStep.Kind.valueOf(kindStr); }
            catch (IllegalArgumentException e) { kind = PlanStep.Kind.MODEL; }
            String target = s.path("target").asText("");

            Map<String, PlanStep.ValueRef> inputs = new LinkedHashMap<>();
            JsonNode in = s.path("inputs");
            if (in.isObject()) {
                in.fields().forEachRemaining(e -> inputs.put(e.getKey(), toRef(e.getValue())));
            }
            List<String> deps = new ArrayList<>();
            for (JsonNode d : s.path("depends_on")) deps.add(d.asText());
            String cond = s.path("condition").asText("");
            steps.add(new PlanStep(id, description, kind, target, inputs, deps, cond));
        }
        if (steps.isEmpty()) return fallback(goal);
        String rootId = root.path("root").asText(steps.get(steps.size() - 1).id());
        return new Plan(goal, steps, rootId);
    }

    private static PlanStep.ValueRef toRef(JsonNode n) {
        if (n.has("literal")) {
            JsonNode lit = n.get("literal");
            if (lit.isTextual()) return new PlanStep.ValueRef.Literal(lit.asText());
            if (lit.isNumber()) return new PlanStep.ValueRef.Literal(lit.numberValue());
            if (lit.isBoolean()) return new PlanStep.ValueRef.Literal(lit.asBoolean());
            return new PlanStep.ValueRef.Literal(Json.stringify(lit));
        }
        if (n.has("step")) {
            return new PlanStep.ValueRef.FromStep(n.get("step").asText(), n.path("path").asText(""));
        }
        if (n.has("memory")) return new PlanStep.ValueRef.FromMemory(n.get("memory").asText());
        if (n.has("ask"))    return new PlanStep.ValueRef.FromUser(n.get("ask").asText());
        return new PlanStep.ValueRef.Literal(n.asText(""));
    }
}
