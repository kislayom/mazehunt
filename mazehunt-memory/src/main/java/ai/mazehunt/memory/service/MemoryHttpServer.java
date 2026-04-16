package ai.mazehunt.memory.service;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.memory.RecallRequest;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.Message;
import ai.mazehunt.core.util.Json;
import ai.mazehunt.memory.api.MemoryDtos;
import ai.mazehunt.memory.compact.CompactionService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Stand-alone HTTP façade in front of any {@link MemoryService} implementation.
 *
 * <p>Routes:
 * <pre>
 *   POST  /v1/memory/items         — remember
 *   POST  /v1/memory/facts         — learn a semantic fact
 *   POST  /v1/memory/recall        — hybrid BM25 + vector recall
 *   POST  /v1/memory/consolidate   — force a consolidation pass
 *   GET   /v1/memory/stats         — per-tier counts
 *   GET   /v1/healthz              — liveness probe
 * </pre>
 *
 * <p>JSON in, JSON out. Zero external web-framework dependencies — uses the
 * JDK's built-in {@link HttpServer}, which is sufficient for single-host
 * deployments and trivially proxyable behind anything serious.
 */
public final class MemoryHttpServer {

    private static final Logger log = LoggerFactory.getLogger(MemoryHttpServer.class);

    private final MemoryService memory;
    private final CompactionService compaction;
    private final EmbeddingClient embedder;
    private final HttpServer server;
    private final long startedAt = System.currentTimeMillis();

    public MemoryHttpServer(MemoryService memory, int port, int threads) throws IOException {
        this(memory, null, null, port, threads);
    }

    public MemoryHttpServer(MemoryService memory,
                            CompactionService compaction,
                            EmbeddingClient embedder,
                            int port, int threads) throws IOException {
        this.memory = memory;
        this.compaction = compaction;
        this.embedder = embedder;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "mazehunt-memory-http");
            t.setDaemon(true);
            return t;
        }));
        route("/v1/memory/items",        "POST", this::handleRemember);
        route("/v1/memory/facts",        "POST", this::handleLearnFact);
        route("/v1/memory/recall",       "POST", this::handleRecall);
        route("/v1/memory/consolidate",  "POST", this::handleConsolidate);
        route("/v1/memory/compact",      "POST", this::handleCompact);
        route("/v1/memory/stats",        "GET",  this::handleStats);
        route("/v1/memory/embedder-info","GET",  this::handleEmbedderInfo);
        route("/v1/healthz",             "GET",  this::handleHealth);
    }

    public void start() {
        server.start();
        log.info("Mazehunt memory service listening on port {}", server.getAddress().getPort());
    }

    public void stop() { server.stop(0); }

    public int port() { return server.getAddress().getPort(); }

    // ---------- routing plumbing ----------

    private void route(String path, String method, HttpHandler delegate) {
        server.createContext(path, ex -> {
            try {
                if (!method.equalsIgnoreCase(ex.getRequestMethod())) {
                    send(ex, 405, new MemoryDtos.ErrorResponseDto("method not allowed"));
                    return;
                }
                delegate.handle(ex);
            } catch (Exception e) {
                log.warn("{} {} failed", method, path, e);
                try { send(ex, 500, new MemoryDtos.ErrorResponseDto(e.toString())); }
                catch (IOException ignored) { /* connection already gone */ }
            }
        });
    }

    // ---------- handlers ----------

    private void handleRemember(HttpExchange ex) throws IOException {
        MemoryDtos.RememberRequestDto req = parse(ex, MemoryDtos.RememberRequestDto.class);
        if (req.content() == null || req.content().isBlank()) {
            send(ex, 400, new MemoryDtos.ErrorResponseDto("content required"));
            return;
        }
        String id = memory.remember(req.content(), req.tags() == null ? Map.of() : req.tags(),
                req.source());
        send(ex, 201, new MemoryDtos.IdResponse(id));
    }

    private void handleLearnFact(HttpExchange ex) throws IOException {
        MemoryDtos.LearnFactRequestDto req = parse(ex, MemoryDtos.LearnFactRequestDto.class);
        if (req.content() == null || req.content().isBlank()) {
            send(ex, 400, new MemoryDtos.ErrorResponseDto("content required"));
            return;
        }
        double conf = req.confidence() == null ? 0.9 : req.confidence();
        String id = memory.learnFact(req.content(), req.source(), conf);
        send(ex, 201, new MemoryDtos.IdResponse(id));
    }

    private void handleRecall(HttpExchange ex) throws IOException {
        MemoryDtos.RecallRequestDto req = parse(ex, MemoryDtos.RecallRequestDto.class);
        RecallRequest r = new RecallRequest(
                req.query() == null ? "" : req.query(),
                req.tokenBudget() == null ? 1200 : req.tokenBudget(),
                req.tiers(),
                req.requiredTags() == null ? Map.of() : req.requiredTags(),
                req.minConfidence() == null ? 0.0 : req.minConfidence());
        List<MemoryItem> items = memory.recall(r);
        List<MemoryDtos.MemoryItemDto> dtos = new ArrayList<>(items.size());
        for (MemoryItem m : items) dtos.add(MemoryDtos.MemoryItemDto.fromDomain(m));
        send(ex, 200, new MemoryDtos.RecallResponseDto(dtos));
    }

    private void handleConsolidate(HttpExchange ex) throws IOException {
        memory.consolidate();
        send(ex, 200, Map.of("ok", true));
    }

    private void handleStats(HttpExchange ex) throws IOException {
        send(ex, 200, new MemoryDtos.StatsResponseDto(memory.stats()));
    }

    private void handleCompact(HttpExchange ex) throws IOException {
        if (compaction == null) {
            send(ex, 503, new MemoryDtos.ErrorResponseDto("compaction not configured"));
            return;
        }
        MemoryDtos.CompactRequestDto req = parse(ex, MemoryDtos.CompactRequestDto.class);
        List<Message> msgs = new ArrayList<>();
        if (req.messages() != null) {
            for (MemoryDtos.MessageDto m : req.messages()) {
                Message.Role role = switch (m.role() == null ? "user" : m.role().toLowerCase()) {
                    case "system"    -> Message.Role.SYSTEM;
                    case "assistant" -> Message.Role.ASSISTANT;
                    default          -> Message.Role.USER;
                };
                msgs.add(new Message(role, List.of(new Message.TextPart(m.text()))));
            }
        }
        int maxTokens = req.maxTokens() == null ? 20000 : req.maxTokens();
        CompactionService.CompactionResult r = compaction.compact(msgs, maxTokens);
        send(ex, 200, new MemoryDtos.CompactResponseDto(
                r.compacted(), r.summary(), r.messagesCompacted(),
                r.inputTokens(), r.outputTokens(), r.skipReason()));
    }

    private void handleEmbedderInfo(HttpExchange ex) throws IOException {
        if (embedder == null) {
            send(ex, 200, new MemoryDtos.EmbedderInfoDto("none", 0, ""));
        } else {
            String type = embedder.getClass().getSimpleName();
            send(ex, 200, new MemoryDtos.EmbedderInfoDto(type, embedder.dimensions(), ""));
        }
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        send(ex, 200, new MemoryDtos.HealthResponseDto("ok",
                System.currentTimeMillis() - startedAt));
    }

    // ---------- helpers ----------

    private static <T> T parse(HttpExchange ex, Class<T> type) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] body = in.readAllBytes();
            if (body.length == 0) return Json.MAPPER.readValue("{}", type);
            return Json.MAPPER.readValue(body, type);
        }
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.MAPPER.writeValueAsBytes(body);
        Headers h = ex.getResponseHeaders();
        h.set("content-type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    /** Convenience constant for tests to reference the content type. */
    public static final String CONTENT_TYPE = "application/json; charset=" + StandardCharsets.UTF_8;
}
