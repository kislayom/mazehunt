package ai.mazehunt.skills.core;

import ai.mazehunt.api.model.ToolSpec;
import ai.mazehunt.api.skill.Skill;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.api.skill.SkillResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Fetch a URL and return (truncated) text — anchors tool outputs to real sources. */
public final class WebFetchSkill implements Skill {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override public String id() { return "web.fetch"; }
    @Override public String description() { return "Retrieves the text of a URL."; }

    @Override
    public List<ToolSpec> tools() {
        return List.of(new ToolSpec("web_fetch",
                "Fetch a URL and return plain-text contents (max 20k chars).",
                Map.of("type", "object",
                        "properties", Map.of("url", Map.of("type", "string")),
                        "required", List.of("url"))));
    }

    @Override
    public SkillResult invoke(String tool, Map<String, Object> args, SkillContext ctx) {
        String url = String.valueOf(args.get("url"));
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("user-agent", "Mazehunt/0.1")
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body.length() > 20000) body = body.substring(0, 20000) + "\n…[truncated]";
            // crude HTML → text
            String text = body.replaceAll("<script[\\s\\S]*?</script>", " ")
                              .replaceAll("<style[\\s\\S]*?</style>", " ")
                              .replaceAll("<[^>]+>", " ")
                              .replaceAll("\\s+", " ").trim();
            return SkillResult.ok(text, List.of(url));
        } catch (Exception e) {
            return SkillResult.error("fetch failed: " + e.getMessage());
        }
    }
}
