package ai.mazehunt.api.plan;

import java.time.Instant;
import java.util.List;

/** Output of executing a single {@link PlanStep}. */
public record StepResult(
        String stepId,
        Status status,
        Object output,
        String error,
        List<String> citations,
        Instant startedAt,
        Instant finishedAt
) {
    public enum Status { PENDING, RUNNING, OK, FAILED, SKIPPED, UNKNOWN }
}
