package ai.mazehunt.core.skill;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.skill.SkillContext;

import java.util.HashMap;
import java.util.Map;

public final class DefaultSkillContext implements SkillContext {
    private final MemoryService memory;
    private final ModelClient model;
    private final Map<String, Object> scratch = new HashMap<>();

    public DefaultSkillContext(MemoryService memory, ModelClient model) {
        this.memory = memory;
        this.model = model;
    }

    @Override public MemoryService memory() { return memory; }
    @Override public ModelClient defaultModel() { return model; }
    @Override public Map<String, Object> scratch() { return scratch; }
}
