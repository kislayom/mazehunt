package ai.mazehunt.memory.compact;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.memory.LayeredMemoryService;
import ai.mazehunt.memory.store.InMemoryVectorIndex;
import ai.mazehunt.memory.store.SqliteMemoryStore;
import ai.mazehunt.memory.testsupport.FakeModelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompactionServiceTest {

    private SqliteMemoryStore store;
    private MemoryService memory;

    @BeforeEach
    void setup(@TempDir Path tmp) {
        store = new SqliteMemoryStore(tmp.resolve("mem.db"));
        memory = new LayeredMemoryService(store, new InMemoryVectorIndex(),
                null, null, 100, 0.0);
    }

    @AfterEach
    void tearDown() { store.close(); }

    @Test
    void compactsLargeConversation() {
        FakeModelClient model = new FakeModelClient(prompt ->
                "## Summary\n- User asked about VRAM setup\n- Decided on 48GB config");
        CompactionService svc = new CompactionService(model, memory);

        // Build a conversation bigger than the target.
        List<Message> msgs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            msgs.add(Message.user("Tell me about point number " + i + " " + "details ".repeat(20)));
            msgs.add(Message.assistant("Here is the answer for " + i + " " + "explanation ".repeat(20)));
        }

        CompactionService.CompactionResult r = svc.compact(msgs, 200);
        assertTrue(r.compacted());
        assertNotNull(r.summary());
        assertTrue(r.summary().contains("VRAM"));
        assertEquals(100, r.messagesCompacted());
        assertTrue(r.inputTokens() > 200, "input should exceed target");
        assertEquals(1, model.invocations.get());
    }

    @Test
    void skipsWhenInputAlreadyUnderBudget() {
        FakeModelClient model = new FakeModelClient("should not be called");
        CompactionService svc = new CompactionService(model, memory);

        List<Message> msgs = List.of(Message.user("hi"), Message.assistant("hello"));
        CompactionService.CompactionResult r = svc.compact(msgs, 5000);
        assertFalse(r.compacted());
        assertNotNull(r.skipReason());
        assertEquals(0, model.invocations.get());
    }

    @Test
    void skipsWhenNoModel() {
        CompactionService svc = new CompactionService(null, memory);
        CompactionService.CompactionResult r = svc.compact(
                List.of(Message.user("hi")), 100);
        assertFalse(r.compacted());
        assertTrue(r.skipReason().contains("no summariser"));
    }

    @Test
    void skipsEmptyMessages() {
        FakeModelClient model = new FakeModelClient("x");
        CompactionService svc = new CompactionService(model, memory);
        assertFalse(svc.compact(List.of(), 100).compacted());
        assertFalse(svc.compact(null, 100).compacted());
    }

    @Test
    void persistsCompactedSessionAsEpisodicMemory() {
        FakeModelClient model = new FakeModelClient(prompt -> "compacted result");
        CompactionService svc = new CompactionService(model, memory);

        List<Message> msgs = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            msgs.add(Message.user("message " + i + " " + "padding ".repeat(20)));
        }
        svc.compact(msgs, 100);

        long episodic = memory.stats().get(MemoryItem.Tier.EPISODIC);
        assertTrue(episodic >= 1, "compacted session should be stored in episodic memory");
    }

    @Test
    void toleratesModelFailure() {
        FakeModelClient model = new FakeModelClient(p -> { throw new RuntimeException("fail"); });
        CompactionService svc = new CompactionService(model, memory);

        List<Message> msgs = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            msgs.add(Message.user("msg " + i + " " + "x ".repeat(20)));
        }
        CompactionService.CompactionResult r = svc.compact(msgs, 50);
        assertFalse(r.compacted());
        assertTrue(r.skipReason().contains("model error"));
    }
}
