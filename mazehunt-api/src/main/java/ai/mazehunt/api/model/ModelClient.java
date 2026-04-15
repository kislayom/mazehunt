package ai.mazehunt.api.model;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Provider-agnostic chat-completion client. */
public interface ModelClient {

    ModelCapability capability();

    /** Blocking completion. */
    ModelResponse complete(ModelRequest request);

    /** Async streaming completion. {@code onToken} receives incremental text chunks. */
    default CompletableFuture<ModelResponse> stream(ModelRequest request, Consumer<String> onToken) {
        return CompletableFuture.supplyAsync(() -> {
            ModelResponse r = complete(request);
            onToken.accept(r.text());
            return r;
        });
    }
}
