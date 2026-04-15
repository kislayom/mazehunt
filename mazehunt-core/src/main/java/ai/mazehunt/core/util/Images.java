package ai.mazehunt.core.util;

import java.util.Base64;

/**
 * Provider-neutral image encoding helpers. Keeps Base64 / data-URI logic out
 * of business-logic classes so the provider adapters stay one-liners.
 */
public final class Images {

    private Images() {}

    /** Raw Base64 (no prefix). Used by Ollama's {@code images} array and Anthropic's source.data. */
    public static String toBase64(byte[] bytes) {
        return bytes == null ? "" : Base64.getEncoder().encodeToString(bytes);
    }

    /** OpenAI-style data URI: {@code data:image/png;base64,AAA…}. */
    public static String toDataUri(byte[] bytes, String mimeType) {
        String mime = mimeType == null || mimeType.isEmpty() ? Mimes.DEFAULT_IMAGE : mimeType;
        return "data:" + mime + ";base64," + toBase64(bytes);
    }
}
