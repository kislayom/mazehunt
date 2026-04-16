package ai.mazehunt.memory.compact;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.core.util.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mid-session context compaction inspired by Claude's approach: when the agent's
 * working context overflows, it calls this service to produce a structured
 * summary that replaces the raw transcript. The summary preserves:
 *
 * <ol>
 *   <li>Key decisions and their rationale.</li>
 *   <li>Open questions the user asked that haven't been answered.</li>
 *   <li>Entities, file paths, numbers — anything the model would need to
 *       continue coherently.</li>
 *   <li>What the assistant was doing when compaction was triggered.</li>
 * </ol>
 *
 * <p>The caller keeps the summary as the new conversation history and drops the
 * raw messages. Critically, every compacted session is also stored as an
 * episodic memory so the full-text recall system can still find it later — the
 * agent doesn't "forget," it just moves detail out of the live prompt.
 */
public final class CompactionService {

    private static final Logger log = LoggerFactory.getLogger(CompactionService.class);

    private static final String COMPACTION_PROMPT = """
            You are a context compactor. Your job is to produce a structured summary
            of the conversation so far. The summary will REPLACE the full conversation
            in the model's context window — so it must preserve every detail the model
            needs to continue working.

            Rules:
            1. Keep all file paths, variable names, error messages, numbers, and dates verbatim.
            2. Keep all decisions and their reasons ("we chose X because Y").
            3. Keep all unanswered user questions.
            4. Keep the current task state ("currently working on…" / "next step is…").
            5. Drop pleasantries, reformulations, and repeated information.
            6. Drop raw tool outputs (file contents, long stack traces) — summarise them.
            7. Output ONLY the summary. No preamble.
            8. Target %d tokens or fewer.

            Conversation to compact:
            %s
            """;

    private static final int RESERVE_BUFFER = 13_000;

    private final ModelClient model;
    private final MemoryService memory;

    public CompactionService(ModelClient model, MemoryService memory) {
        this.model = model;
        this.memory = memory;
    }

    /**
     * Compact a conversation transcript.
     *
     * @param messages   raw conversation messages
     * @param maxTokens  target token budget for the summary
     * @return           the compacted result
     */
    public CompactionResult compact(List<Message> messages, int maxTokens) {
        if (model == null) {
            return CompactionResult.skipped("no summariser model configured");
        }
        if (messages == null || messages.isEmpty()) {
            return CompactionResult.skipped("nothing to compact");
        }

        int inputTokens = 0;
        StringBuilder transcript = new StringBuilder();
        for (Message m : messages) {
            String line = m.role().wire() + ": " + m.text() + "\n";
            transcript.append(line);
            inputTokens += Tokens.count(line);
        }
        if (inputTokens < maxTokens) {
            return CompactionResult.skipped("input (" + inputTokens +
                    " tokens) already under target (" + maxTokens + ")");
        }

        String prompt = COMPACTION_PROMPT.formatted(maxTokens, transcript);
        try {
            String summary = model.complete(
                    ModelRequest.of(List.of(Message.user(prompt)))).text();
            int summaryTokens = Tokens.count(summary);

            // Persist the compacted transcript so recall can still find it.
            memory.remember("COMPACTED SESSION:\n" + summary,
                    Map.of("kind", "compaction"), "compaction");

            log.info("Compacted {} tokens → {} tokens ({} messages)",
                    inputTokens, summaryTokens, messages.size());
            return new CompactionResult(true, summary, messages.size(),
                    inputTokens, summaryTokens, null);
        } catch (RuntimeException e) {
            log.warn("Compaction failed: {}", e.toString());
            return CompactionResult.skipped("model error: " + e.getMessage());
        }
    }

    /**
     * Auto-compact: check if a conversation is near a context limit and compact
     * proactively if needed. Returns the compacted summary or null if no
     * compaction was necessary.
     */
    public CompactionResult autoCompact(List<Message> messages,
                                        int contextWindowTokens) {
        int used = 0;
        for (Message m : messages) used += Tokens.count(m.text());

        // Also pull in recalled memory budget to approximate true prompt size.
        List<MemoryItem> recalled = memory.recall(RecallRequest.of("", 100));
        for (MemoryItem mi : recalled) used += mi.tokens();

        if (used < contextWindowTokens - RESERVE_BUFFER) {
            return CompactionResult.skipped("within budget (" + used +
                    "/" + contextWindowTokens + ")");
        }
        int target = (contextWindowTokens - RESERVE_BUFFER) / 2;
        return compact(messages, Math.max(2000, target));
    }

    public record CompactionResult(
            boolean compacted,
            String summary,
            int messagesCompacted,
            int inputTokens,
            int outputTokens,
            String skipReason
    ) {
        static CompactionResult skipped(String reason) {
            return new CompactionResult(false, null, 0, 0, 0, reason);
        }
    }
}
