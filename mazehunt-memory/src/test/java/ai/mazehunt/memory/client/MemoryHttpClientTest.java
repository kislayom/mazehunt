package ai.mazehunt.memory.client;

import ai.mazehunt.api.memory.RecallRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Client-only tests: how does {@link MemoryHttpClient} behave when the server
 * is down, slow, or returns non-JSON? These are deliberately isolated from the
 * real service so we can force failure modes.
 */
class MemoryHttpClientTest {

    @Test
    void unreachableServerRaisesWrappedException() throws Exception {
        int port = freePort();
        MemoryHttpClient client = new MemoryHttpClient("http://localhost:" + port,
                Duration.ofMillis(500));

        var ex = assertThrows(MemoryHttpClient.MemoryServiceException.class,
                () -> client.stats());
        assertTrue(ex.getMessage().toLowerCase().contains("memory service"));
    }

    @Test
    void serverReturningBrokenJsonFailsCleanly() throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress(0), 0);
        srv.setExecutor(Executors.newSingleThreadExecutor());
        srv.createContext("/v1/memory/stats", ex -> {
            byte[] body = "not-json".getBytes();
            ex.getResponseHeaders().set("content-type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        srv.start();
        try {
            MemoryHttpClient client = new MemoryHttpClient(
                    "http://localhost:" + srv.getAddress().getPort(),
                    Duration.ofSeconds(2));
            assertThrows(MemoryHttpClient.MemoryServiceException.class, client::stats);
        } finally {
            srv.stop(0);
        }
    }

    @Test
    void nonOkStatusIsPropagatedWithBody() throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress(0), 0);
        srv.setExecutor(Executors.newSingleThreadExecutor());
        srv.createContext("/v1/memory/recall", ex -> {
            byte[] body = "{\"error\":\"synthetic failure\"}".getBytes();
            ex.getResponseHeaders().set("content-type", "application/json");
            ex.sendResponseHeaders(503, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        srv.start();
        try {
            MemoryHttpClient client = new MemoryHttpClient(
                    "http://localhost:" + srv.getAddress().getPort(),
                    Duration.ofSeconds(2));
            var ex = assertThrows(MemoryHttpClient.MemoryServiceException.class,
                    () -> client.recall(RecallRequest.of("q", 100)));
            assertTrue(ex.getMessage().contains("503"));
            assertTrue(ex.getMessage().contains("synthetic failure"));
        } finally {
            srv.stop(0);
        }
    }

    @Test
    void trailingSlashInBaseUrlIsNormalised() throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress(0), 0);
        srv.setExecutor(Executors.newSingleThreadExecutor());
        srv.createContext("/v1/memory/items", ex -> {
            byte[] body = "{\"id\":\"abc\"}".getBytes();
            ex.getResponseHeaders().set("content-type", "application/json");
            ex.sendResponseHeaders(201, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        srv.start();
        try {
            MemoryHttpClient client = new MemoryHttpClient(
                    "http://localhost:" + srv.getAddress().getPort() + "/",
                    Duration.ofSeconds(2));
            // A duplicated slash in the URL would cause HttpServer to 404.
            assertEquals("abc", client.remember("hi", Map.of(), "src"));
        } finally {
            srv.stop(0);
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }
}
