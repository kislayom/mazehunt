package ai.mazehunt.api.plan;

import java.util.List;
import java.util.Map;

/**
 * One node in the execution DAG. {@code inputs} is a map from parameter name to
 * a {@link ValueRef} — either a literal or a pointer to another step's output —
 * which is how data flows between steps.
 */
public record PlanStep(
        String id,
        String description,
        Kind kind,
        /** For TOOL kind: skill id / tool name. For MODEL kind: route hint. */
        String target,
        Map<String, ValueRef> inputs,
        /** Step ids this step depends on (derived from ValueRef.step() but kept explicit). */
        List<String> dependsOn,
        /** Optional gate: natural-language condition evaluated by the model. */
        String condition
) {
    public enum Kind {
        /** Invoke a registered tool / skill. */
        TOOL,
        /** Free-form model reasoning step. */
        MODEL,
        /** Recall from memory. */
        RECALL,
        /** Ask the user a clarifying question. */
        ASK_USER,
        /** Fan-out over a collection (map step). */
        MAP,
        /** Reduce / aggregate results. */
        REDUCE
    }

    public sealed interface ValueRef {
        record Literal(Object value) implements ValueRef {}
        record FromStep(String step, String path) implements ValueRef {}
        record FromMemory(String query) implements ValueRef {}
        record FromUser(String prompt) implements ValueRef {}
    }
}
