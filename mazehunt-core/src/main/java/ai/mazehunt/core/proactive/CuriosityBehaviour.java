package ai.mazehunt.core.proactive;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.api.proactive.ProactiveBehaviour;
import ai.mazehunt.api.proactive.ProactiveContext;

import java.time.Duration;
import java.util.List;

/**
 * Scans the knowledge graph for gaps or contradictions and asks the user at
 * most one clarifying question per tick. This is what makes the agent "learn
 * more about you without annoying you".
 */
public final class CuriosityBehaviour implements ProactiveBehaviour {

    @Override public String name() { return "curiosity.gap-filler"; }
    @Override public Duration interval() { return Duration.ofHours(2); }

    @Override
    public void tick(ProactiveContext ctx) {
        List<MemoryItem> profile = ctx.memory().recall(
                RecallRequest.of("user profile preferences goals ongoing projects", 800));
        StringBuilder known = new StringBuilder();
        for (MemoryItem m : profile) known.append("- ").append(m.content()).append('\n');

        String prompt = """
                You are generating ONE question to ask the user that would
                meaningfully reduce uncertainty about them. Rules:
                - Do not ask what we already know (see below).
                - Prefer concrete, low-friction questions.
                - If uncertainty is already low, output exactly the token NONE.
                Known about the user:
                %s
                Output just the question or NONE.
                """.formatted(known);
        String q;
        try {
            q = ctx.defaultModel().complete(
                    ModelRequest.of(List.of(Message.user(prompt)))).text();
        } catch (RuntimeException e) { return; }
        if (q == null) return;
        String trimmed = q.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("NONE")) return;
        ctx.nudge(trimmed);
    }
}
