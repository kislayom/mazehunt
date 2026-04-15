package ai.mazehunt.core.plan;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.api.plan.Plan;
import ai.mazehunt.api.plan.PlanStep;
import ai.mazehunt.api.plan.StepResult;
import ai.mazehunt.api.skill.Skill;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.api.skill.SkillResult;
import ai.mazehunt.core.skill.SkillRegistry;
import ai.mazehunt.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Topologically executes a {@link Plan}, threading each step's output into
 * downstream {@link PlanStep.ValueRef.FromStep} references.
 *
 * <p>The executor enforces the anti-hallucination contract: when a step cannot
 * produce a fact-supported answer, its {@link StepResult#status()} becomes
 * {@link StepResult.Status#UNKNOWN} and is surfaced to the user rather than
 * papered over with a guess.
 */
public final class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final MemoryService memory;
    private final SkillRegistry skills;
    private final ModelClient model;
    private final Function<String, String> askUser;  // prompt → user answer

    public PlanExecutor(MemoryService memory, SkillRegistry skills, ModelClient model,
                        Function<String, String> askUser) {
        this.memory = memory;
        this.skills = skills;
        this.model = model;
        this.askUser = askUser;
    }

    public List<StepResult> execute(Plan plan, SkillContext ctx) {
        Map<String, StepResult> results = new LinkedHashMap<>();
        for (String id : topoSort(plan)) {
            PlanStep step = findStep(plan, id);
            Instant started = Instant.now();
            try {
                Object out = runStep(step, results, ctx);
                results.put(id, new StepResult(id, StepResult.Status.OK, out, null,
                        List.of(), started, Instant.now()));
            } catch (UnknownAnswerException uae) {
                log.info("Step {} returned UNKNOWN: {}", id, uae.getMessage());
                results.put(id, new StepResult(id, StepResult.Status.UNKNOWN, null,
                        uae.getMessage(), List.of(), started, Instant.now()));
            } catch (RuntimeException e) {
                log.warn("Step {} failed: {}", id, e.toString());
                results.put(id, new StepResult(id, StepResult.Status.FAILED, null,
                        e.getMessage(), List.of(), started, Instant.now()));
            }
        }
        return new ArrayList<>(results.values());
    }

    private Object runStep(PlanStep step, Map<String, StepResult> prior, SkillContext ctx) {
        Map<String, Object> inputs = resolveInputs(step, prior);
        return switch (step.kind()) {
            case TOOL     -> runTool(step, inputs, ctx);
            case MODEL    -> runModel(step, inputs);
            case RECALL   -> runRecall(step, inputs);
            case ASK_USER -> runAsk(step, inputs);
            case MAP      -> runMap(step, inputs, ctx);
            case REDUCE   -> runReduce(step, inputs);
        };
    }

    private Object runTool(PlanStep step, Map<String, Object> inputs, SkillContext ctx) {
        Skill skill = skills.skillOwningTool(step.target())
                .orElseThrow(() -> new IllegalArgumentException("No skill for tool: " + step.target()));
        SkillResult r = skill.invoke(step.target(), inputs, ctx);
        if (!r.success()) throw new RuntimeException("tool failed: " + r.errorMessage());
        return r.value();
    }

    private Object runModel(PlanStep step, Map<String, Object> inputs) {
        String prompt = "Step: %s\nInputs: %s\nRespond with the answer only."
                .formatted(step.description(), Json.stringify(inputs));
        return model.complete(ModelRequest.of(List.of(Message.user(prompt)))).text();
    }

    private Object runRecall(PlanStep step, Map<String, Object> inputs) {
        String q = String.valueOf(inputs.getOrDefault("query",
                inputs.values().stream().findFirst().orElse(step.description())));
        List<MemoryItem> items = memory.recall(RecallRequest.of(q, 1000));
        if (items.isEmpty()) throw new UnknownAnswerException("no relevant memory for: " + q);
        return items;
    }

    private Object runAsk(PlanStep step, Map<String, Object> inputs) {
        String q = String.valueOf(inputs.getOrDefault("question", step.description()));
        return askUser.apply(q);
    }

    private Object runMap(PlanStep step, Map<String, Object> inputs, SkillContext ctx) {
        Object collection = inputs.get("items");
        if (!(collection instanceof Iterable<?> it)) {
            throw new IllegalArgumentException("MAP step needs 'items' iterable");
        }
        List<Object> out = new ArrayList<>();
        for (Object elem : it) {
            String prompt = "Apply: %s\nElement: %s".formatted(step.description(), elem);
            out.add(model.complete(ModelRequest.of(List.of(Message.user(prompt)))).text());
        }
        return out;
    }

    private Object runReduce(PlanStep step, Map<String, Object> inputs) {
        String prompt = "Reduce/aggregate: %s\nInputs: %s"
                .formatted(step.description(), Json.stringify(inputs));
        return model.complete(ModelRequest.of(List.of(Message.user(prompt)))).text();
    }

    // ------------ input resolution ------------

    private Map<String, Object> resolveInputs(PlanStep step, Map<String, StepResult> prior) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, PlanStep.ValueRef> e : step.inputs().entrySet()) {
            out.put(e.getKey(), resolve(e.getValue(), prior));
        }
        return out;
    }

    private Object resolve(PlanStep.ValueRef ref, Map<String, StepResult> prior) {
        return switch (ref) {
            case PlanStep.ValueRef.Literal l    -> l.value();
            case PlanStep.ValueRef.FromStep fs  -> extractPath(prior.get(fs.step()), fs.path());
            case PlanStep.ValueRef.FromMemory m -> memory.recall(RecallRequest.of(m.query(), 600));
            case PlanStep.ValueRef.FromUser u   -> askUser.apply(u.prompt());
        };
    }

    private static Object extractPath(StepResult src, String path) {
        if (src == null) return null;
        if (src.status() != StepResult.Status.OK) return null;
        Object v = src.output();
        if (path == null || path.isEmpty()) return v;
        // Very small JSONPath: dot-separated field access on Map / JsonNode.
        for (String seg : path.split("\\.")) {
            if (seg.isEmpty()) continue;
            if (v instanceof Map<?, ?> map) v = map.get(seg);
            else if (v instanceof com.fasterxml.jackson.databind.JsonNode n) v = n.path(seg);
            else return v;
        }
        return v;
    }

    // ------------ topo sort ------------

    private static List<String> topoSort(Plan plan) {
        Map<String, List<String>> adj = new LinkedHashMap<>();
        Map<String, Integer> indeg = new LinkedHashMap<>();
        Map<String, PlanStep> byId = new LinkedHashMap<>();
        for (PlanStep s : plan.steps()) {
            byId.put(s.id(), s);
            adj.putIfAbsent(s.id(), new ArrayList<>());
            indeg.putIfAbsent(s.id(), 0);
        }
        for (PlanStep s : plan.steps()) {
            for (String d : s.dependsOn()) {
                if (!byId.containsKey(d)) continue;
                adj.get(d).add(s.id());
                indeg.merge(s.id(), 1, Integer::sum);
            }
        }
        Deque<String> q = new ArrayDeque<>();
        indeg.forEach((k, v) -> { if (v == 0) q.add(k); });
        List<String> out = new ArrayList<>();
        while (!q.isEmpty()) {
            String cur = q.poll();
            out.add(cur);
            for (String next : adj.get(cur)) {
                if (indeg.merge(next, -1, Integer::sum) == 0) q.add(next);
            }
        }
        if (out.size() != byId.size()) {
            // Cycle — fall back to declaration order.
            out.clear();
            for (PlanStep s : plan.steps()) out.add(s.id());
        }
        return out;
    }

    private static PlanStep findStep(Plan plan, String id) {
        for (PlanStep s : plan.steps()) if (s.id().equals(id)) return s;
        throw new NoSuchElementException(id);
    }

    /** Thrown by a step to indicate grounded-fact-missing → surface as UNKNOWN. */
    public static final class UnknownAnswerException extends RuntimeException {
        public UnknownAnswerException(String m) { super(m); }
    }
}
