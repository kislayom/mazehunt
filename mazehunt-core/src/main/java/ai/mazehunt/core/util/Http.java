package ai.mazehunt.core.util;

/** Tiny HTTP helpers shared across provider adapters. */
public final class Http {

    private Http() {}

    /** Strip one trailing slash — lets adapters concatenate {@code endpoint + path}. */
    public static String normaliseEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) return endpoint;
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }
}
