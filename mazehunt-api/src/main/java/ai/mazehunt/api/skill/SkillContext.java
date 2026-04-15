package ai.mazehunt.api.skill;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.ModelClient;

/** Services made available to a {@link Skill} when it runs. */
public interface SkillContext {
    MemoryService memory();
    ModelClient defaultModel();
    /** Session-scoped scratch space — e.g. state shared between steps of a plan. */
    java.util.Map<String, Object> scratch();
}
