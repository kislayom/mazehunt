package ai.mazehunt.api.skill;

import ai.mazehunt.api.model.ToolSpec;

/**
 * A pluggable capability. Discovered via {@link java.util.ServiceLoader} so
 * third-party JARs can drop onto the classpath with a
 * {@code META-INF/services/ai.mazehunt.api.skill.Skill} entry.
 */
public interface Skill {

    /** Stable identifier (e.g. {@code "stocks.buffett"}). */
    String id();

    /** One-line user-facing description. */
    String description();

    /** Tool descriptors this skill contributes to model prompts. */
    java.util.List<ToolSpec> tools();

    /** Execute one of the tools owned by this skill. */
    SkillResult invoke(String tool, java.util.Map<String, Object> arguments, SkillContext ctx);
}
