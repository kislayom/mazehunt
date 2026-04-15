package ai.mazehunt.api.plan;

import java.util.List;

/** An executable flowchart produced by the planner for a given goal. */
public record Plan(String goal, List<PlanStep> steps, String rootStepId) {

    /** Mermaid-flavoured flowchart — useful for logging / UI rendering. */
    public String toMermaid() {
        StringBuilder sb = new StringBuilder("flowchart TD\n");
        sb.append("  goal[\"").append(escape(goal)).append("\"]\n");
        for (PlanStep s : steps) {
            sb.append("  ").append(s.id())
              .append("[\"").append(s.kind()).append(": ").append(escape(s.description())).append("\"]\n");
        }
        for (PlanStep s : steps) {
            for (String dep : s.dependsOn()) {
                sb.append("  ").append(dep).append(" --> ").append(s.id()).append('\n');
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\n", " ");
    }
}
