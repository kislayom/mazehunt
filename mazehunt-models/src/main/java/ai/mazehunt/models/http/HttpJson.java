package ai.mazehunt.models.http;

import ai.mazehunt.core.util.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Tiny JSON-over-HTTP helper used by all provider adapters. */
public final class HttpJson {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private HttpJson() {}

    public static JsonNode post(String url, Object body, Map<String, String> headers, Duration timeout) {
        String json = Json.stringify(body);
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (headers != null) headers.forEach(rb::header);
        try {
            HttpResponse<String> resp = CLIENT.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return Json.tree(resp.body());
        } catch (Exception e) {
            throw new RuntimeException("POST " + url + " failed: " + e.getMessage(), e);
        }
    }
}
