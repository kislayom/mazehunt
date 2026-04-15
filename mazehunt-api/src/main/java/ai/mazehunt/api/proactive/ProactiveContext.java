package ai.mazehunt.api.proactive;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.ModelClient;

/** Services available to a {@link ProactiveBehaviour} during its tick. */
public interface ProactiveContext {
    MemoryService memory();
    ModelClient defaultModel();
    /** Queue a user-visible nudge (question, suggestion) for the next interaction. */
    void nudge(String message);
}
