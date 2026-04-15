package ai.mazehunt.skills.core;

import ai.mazehunt.api.model.ToolSpec;
import ai.mazehunt.api.skill.Skill;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.api.skill.SkillResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Read-only filesystem access confined to a base directory. Write tools are
 * deliberately omitted — destructive actions should go through a dedicated,
 * guarded skill.
 */
public final class FileSystemSkill implements Skill {

    private final Path base;

    public FileSystemSkill() { this(Path.of(System.getProperty("user.home"))); }
    public FileSystemSkill(Path base) { this.base = base.toAbsolutePath().normalize(); }

    @Override public String id() { return "fs.read"; }
    @Override public String description() { return "Read-only filesystem access under a configured base directory."; }

    @Override
    public List<ToolSpec> tools() {
        return List.of(
                new ToolSpec("fs_read", "Read a text file.", Map.of(
                        "type", "object",
                        "properties", Map.of("path", Map.of("type", "string")),
                        "required", List.of("path"))),
                new ToolSpec("fs_list", "List entries of a directory.", Map.of(
                        "type", "object",
                        "properties", Map.of("path", Map.of("type", "string")),
                        "required", List.of("path"))));
    }

    @Override
    public SkillResult invoke(String tool, Map<String, Object> args, SkillContext ctx) {
        try {
            Path p = resolve(args.get("path"));
            return switch (tool) {
                case "fs_read" -> SkillResult.ok(Files.readString(p), List.of(p.toString()));
                case "fs_list" -> SkillResult.ok(Files.list(p).map(Path::toString).toList(),
                                                 List.of(p.toString()));
                default -> SkillResult.error("unknown tool: " + tool);
            };
        } catch (Exception e) {
            return SkillResult.error(e.getMessage());
        }
    }

    private Path resolve(Object raw) {
        Path requested = Path.of(String.valueOf(raw)).toAbsolutePath().normalize();
        if (!requested.startsWith(base)) {
            throw new SecurityException("path escapes base dir: " + requested);
        }
        return requested;
    }
}
