package ai.mazehunt.memory.testsupport;

import ai.mazehunt.api.Modality;
import ai.mazehunt.api.model.ModelCapability;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.api.model.ModelResponse;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Canned {@link ModelClient} for tests. Stores every prompt for assertion and
 * returns whatever the response function produces — lets individual tests
 * fake summariser output (e.g. newline-separated facts for consolidation).
 */
public final class FakeModelClient implements ModelClient {

    private final Function<String, String> responder;
    public final AtomicInteger invocations = new AtomicInteger();
    public final List<String> capturedPrompts = new java.util.concurrent.CopyOnWriteArrayList<>();

    public FakeModelClient(String canned) {
        this(prompt -> canned);
    }

    public FakeModelClient(Function<String, String> responder) {
        this.responder = responder;
    }

    @Override
    public ModelCapability capability() {
        return new ModelCapability("fake-model", "fake",
                Set.of(Modality.TEXT), Set.of(Modality.TEXT),
                8192, false, true, 0, 0, 10);
    }

    @Override
    public ModelResponse complete(ModelRequest request) {
        invocations.incrementAndGet();
        String prompt = request.messages().isEmpty() ? "" : request.messages().get(0).text();
        capturedPrompts.add(prompt);
        return ModelResponse.text(responder.apply(prompt), ModelResponse.Usage.zero());
    }
}
