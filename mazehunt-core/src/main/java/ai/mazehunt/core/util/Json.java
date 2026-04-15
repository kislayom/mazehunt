package ai.mazehunt.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private Json() {}

    public static String stringify(Object o) {
        try { return MAPPER.writeValueAsString(o); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static <T> T parse(String s, Class<T> type) {
        try { return MAPPER.readValue(s, type); }
        catch (Exception e) { throw new IllegalStateException("Could not parse: " + s, e); }
    }

    public static JsonNode tree(String s) {
        try { return MAPPER.readTree(s); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMap(String s) {
        return parse(s, Map.class);
    }
}
