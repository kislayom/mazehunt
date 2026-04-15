package ai.mazehunt.api.model;

import java.util.List;

/**
 * Chat message. {@code parts} lets us carry multi-modal content (text, inline
 * images, audio) without committing to a provider-specific envelope.
 */
public record Message(Role role, List<Part> parts) {

    public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

    public sealed interface Part permits TextPart, ImagePart, AudioPart, ToolResultPart {}

    public record TextPart(String text) implements Part {}
    public record ImagePart(byte[] bytes, String mimeType) implements Part {}
    public record AudioPart(byte[] bytes, String mimeType) implements Part {}
    public record ToolResultPart(String toolCallId, String content, boolean isError) implements Part {}

    public static Message system(String text) {
        return new Message(Role.SYSTEM, List.of(new TextPart(text)));
    }

    public static Message user(String text) {
        return new Message(Role.USER, List.of(new TextPart(text)));
    }

    public static Message assistant(String text) {
        return new Message(Role.ASSISTANT, List.of(new TextPart(text)));
    }

    /** Concatenation of all {@link TextPart}s — handy for providers that only accept strings. */
    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Part p : parts) {
            if (p instanceof TextPart t) sb.append(t.text());
        }
        return sb.toString();
    }
}
