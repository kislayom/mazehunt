package ai.mazehunt.core.skill;

import ai.mazehunt.api.model.ToolSpec;
import ai.mazehunt.api.skill.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry populated from {@link java.util.ServiceLoader} so third-party skill
 * JARs can plug in without code changes.
 */
public final class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, Skill> byId = new LinkedHashMap<>();
    private final Map<String, Skill> byTool = new LinkedHashMap<>();

    public static SkillRegistry discoverFromServiceLoader() {
        SkillRegistry r = new SkillRegistry();
        for (Skill s : ServiceLoader.load(Skill.class)) r.register(s);
        return r;
    }

    public void register(Skill s) {
        byId.put(s.id(), s);
        for (ToolSpec t : s.tools()) byTool.put(t.name(), s);
        log.info("Skill registered: {} (tools: {})",
                s.id(),
                s.tools().stream().map(ToolSpec::name).toList());
    }

    public Optional<Skill> skillOwningTool(String toolName) {
        return Optional.ofNullable(byTool.get(toolName));
    }

    public Collection<Skill> all() { return byId.values(); }

    public List<ToolSpec> allTools() {
        List<ToolSpec> out = new ArrayList<>();
        for (Skill s : byId.values()) out.addAll(s.tools());
        return out;
    }

    /** Short catalogue used in planner prompts. */
    public String describeForPrompt() {
        StringBuilder sb = new StringBuilder();
        for (ToolSpec t : allTools()) {
            sb.append("- ").append(t.name()).append(": ").append(t.description()).append('\n');
        }
        if (sb.length() == 0) sb.append("(no tools registered)\n");
        return sb.toString();
    }
}
