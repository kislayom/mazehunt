package ai.mazehunt.core.proactive;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.proactive.ProactiveBehaviour;
import ai.mazehunt.api.proactive.ProactiveContext;
import ai.mazehunt.api.proactive.ProactiveEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Single-threaded scheduler that runs registered behaviours at their declared
 * cadence. Keeps an outbox of user-visible nudges drained by the CLI/UI.
 */
public final class DefaultProactiveEngine implements ProactiveEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultProactiveEngine.class);

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(
            1, r -> { Thread t = new Thread(r, "mazehunt-proactive"); t.setDaemon(true); return t; });

    private final MemoryService memory;
    private final ModelClient model;
    private final BlockingQueue<String> outbox = new LinkedBlockingQueue<>();
    private final List<ProactiveBehaviour> behaviours = new ArrayList<>();
    private final int maxNudgesPerHour;
    private final Deque<Instant> nudgeHistory = new ArrayDeque<>();

    public DefaultProactiveEngine(MemoryService memory, ModelClient model, int maxNudgesPerHour) {
        this.memory = memory;
        this.model = model;
        this.maxNudgesPerHour = maxNudgesPerHour;
    }

    @Override
    public void start() {
        for (ProactiveBehaviour b : behaviours) {
            long ms = Math.max(1000, b.interval().toMillis());
            exec.scheduleAtFixedRate(() -> safeTick(b), ms, ms, TimeUnit.MILLISECONDS);
            log.info("Proactive behaviour scheduled: {} every {}", b.name(), b.interval());
        }
    }

    @Override public void stop() { exec.shutdownNow(); }

    @Override
    public void schedule(String name, Duration delay, Runnable action) {
        exec.schedule(() -> {
            try { action.run(); }
            catch (RuntimeException e) { log.warn("scheduled {} failed: {}", name, e.toString()); }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override public void register(ProactiveBehaviour b) { behaviours.add(b); }

    /** Drain pending nudges for the UI. */
    public List<String> drainNudges() {
        List<String> out = new ArrayList<>();
        outbox.drainTo(out);
        return out;
    }

    private void safeTick(ProactiveBehaviour b) {
        try { b.tick(new Ctx()); }
        catch (RuntimeException e) { log.warn("behaviour {} failed: {}", b.name(), e.toString()); }
    }

    private final class Ctx implements ProactiveContext {
        @Override public MemoryService memory() { return memory; }
        @Override public ModelClient defaultModel() { return model; }
        @Override public void nudge(String message) {
            Instant cutoff = Instant.now().minus(Duration.ofHours(1));
            nudgeHistory.removeIf(t -> t.isBefore(cutoff));
            if (nudgeHistory.size() >= maxNudgesPerHour) {
                log.debug("Nudge suppressed (rate-limited): {}", message);
                return;
            }
            nudgeHistory.addLast(Instant.now());
            outbox.offer(message);
        }
    }
}
