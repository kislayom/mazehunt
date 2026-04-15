package ai.mazehunt.memory.service;

import ai.mazehunt.api.memory.MemoryService;
import ai.mazehunt.api.model.EmbeddingClient;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.core.config.ConfigLoader;
import ai.mazehunt.core.config.MazehuntConfig;
import ai.mazehunt.memory.MemoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Runs the memory tier as an independent service. The agent process then
 * connects via {@code MemoryFactory.remote("http://host:port")} instead of
 * embedding the SQLite store in its own JVM.
 *
 * <p>Model clients (for embeddings and consolidation summarisation) are
 * optional — if they are not reachable the service still starts and works as a
 * pure key-value + FTS store. Embeddings must then be supplied by the caller.
 *
 * <p>Usage:
 * <pre>
 *   java -jar mazehunt-memory-service-standalone.jar [config.yaml] [--port 8765] [--no-models]
 * </pre>
 */
public final class MemoryServiceMain {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceMain.class);

    public static void main(String[] args) throws Exception {
        Path configPath = Path.of("config/mazehunt.yaml");
        int port = 8765;
        boolean wireModels = true;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--port"      -> port = Integer.parseInt(args[++i]);
                case "--no-models" -> wireModels = false;
                default -> {
                    if (!a.startsWith("--")) configPath = Path.of(a);
                }
            }
        }

        MazehuntConfig cfg = ConfigLoader.loadOrDefault(configPath);

        EmbeddingClient embedder = null;
        ModelClient summariser = null;
        if (wireModels) {
            try {
                var mc = tryWireModels(cfg);
                embedder = mc.embedder();
                summariser = mc.summariser();
            } catch (RuntimeException e) {
                log.warn("Starting memory service without model clients: {}", e.toString());
            }
        }

        MemoryService memory = MemoryFactory.local(
                Path.of(cfg.memory().sqlitePath()),
                embedder,
                summariser,
                cfg.memory().consolidateEveryNTurns(),
                cfg.memory().confidenceFloor());

        MemoryHttpServer server = new MemoryHttpServer(memory, port,
                Math.max(4, Runtime.getRuntime().availableProcessors()));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "mazehunt-memory-shutdown"));

        // Block forever.
        Thread.currentThread().join();
    }

    /**
     * Reflectively wires model clients from {@code mazehunt-models} if that
     * module is on the classpath. Kept reflective so the memory service itself
     * does not have a hard dependency on the models module.
     */
    private static ModelBundle tryWireModels(MazehuntConfig cfg) {
        try {
            Class<?> factory = Class.forName("ai.mazehunt.models.ModelFactory");
            Class<?> routerCls = Class.forName("ai.mazehunt.core.router.ModelRouter");
            Object router = routerCls.getConstructor(boolean.class, int.class, int.class)
                    .newInstance(cfg.router().preferLocal(),
                                 cfg.router().cloudFallbackMinContext(),
                                 cfg.router().cloudFallbackComplexity());
            factory.getMethod("registerAll", MazehuntConfig.class, routerCls)
                   .invoke(null, cfg, router);
            Class<?> role = Class.forName("ai.mazehunt.core.router.ModelRouter$Role");
            Class<?> task = Class.forName("ai.mazehunt.core.router.ModelRouter$Task");
            Object lightTask = task.getMethod("light").invoke(null);
            Object embedRole = Enum.valueOf(role.asSubclass(Enum.class), "EMBEDDING");
            Object fastRole  = Enum.valueOf(role.asSubclass(Enum.class), "FAST");
            ModelClient embedModel = (ModelClient) routerCls.getMethod("pick", role, task)
                    .invoke(router, embedRole, lightTask);
            ModelClient fast = (ModelClient) routerCls.getMethod("pick", role, task)
                    .invoke(router, fastRole, lightTask);
            EmbeddingClient embedder = embedModel instanceof EmbeddingClient ec ? ec : null;
            return new ModelBundle(embedder, fast);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private record ModelBundle(EmbeddingClient embedder, ModelClient summariser) {}
}
