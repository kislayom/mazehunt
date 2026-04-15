package ai.mazehunt.core.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void httpNormaliseEndpoint() {
        assertEquals("http://x", Http.normaliseEndpoint("http://x/"));
        assertEquals("http://x", Http.normaliseEndpoint("http://x"));
        assertNull(Http.normaliseEndpoint(null));
    }

    @Test
    void imagesEncoding() {
        byte[] b = {1, 2, 3};
        assertEquals("AQID", Images.toBase64(b));
        assertEquals("data:image/png;base64,AQID", Images.toDataUri(b, null));
        assertEquals("data:image/jpeg;base64,AQID", Images.toDataUri(b, "image/jpeg"));
    }

    @Test
    void mimesMapping() {
        assertEquals("image/jpeg", Mimes.imageMimeFromName("photo.JPG"));
        assertEquals("image/png", Mimes.imageMimeFromName("unknown"));
        assertEquals(".wav", Mimes.audioExtension("audio/wav"));
        assertEquals(".mp3", Mimes.audioExtension("audio/mpeg"));
        assertEquals(".bin", Mimes.audioExtension("weird"));
    }

    @Test
    void tagsRoundTrip() {
        Map<String, String> in = Map.of("k", "v", "a", "b");
        String s = Tags.encode(in);
        Map<String, String> out = Tags.decode(s);
        assertEquals(in, out);
        assertNull(Tags.encode(Map.of()));
    }

    @Test
    void ftsSanitise() {
        assertEquals("\"hello\" OR \"world\"", Fts.sanitiseMatch("hello; world!"));
        assertEquals("\"\"", Fts.sanitiseMatch(""));
        assertEquals("\"\"", Fts.sanitiseMatch(null));
    }

    @Test
    void vectorsFromJson() {
        ArrayNode arr = JsonNodeFactory.instance.arrayNode().add(1.0).add(2.5).add(-3.0);
        float[] v = Vectors.fromJsonArray(arr);
        assertArrayEquals(new float[]{1.0f, 2.5f, -3.0f}, v, 1e-6f);
    }

    @Test
    void topoSort() {
        record Node(String id, List<String> deps) {}
        var nodes = List.of(
                new Node("c", List.of("a", "b")),
                new Node("a", List.of()),
                new Node("b", List.of("a")));
        var ordered = TopoSort.sort(nodes, Node::id, Node::deps);
        assertEquals("a", ordered.get(0).id());
        assertEquals("b", ordered.get(1).id());
        assertEquals("c", ordered.get(2).id());
    }

    @Test
    void jsonExtract() {
        assertEquals("{\"x\":1}", JsonExtract.extractObject("blah {\"x\":1} trailing"));
        assertEquals(1, JsonExtract.path(Map.of("a", Map.of("b", 1)), "a.b"));
    }

    @Test
    void numbersHelpers() {
        assertEquals(2.0, Numbers.avg(new double[]{1, 2, 3}), 1e-9);
        assertArrayEquals(new double[]{1, 2}, Numbers.toDoubles(List.of(1, 2)), 1e-9);
        assertTrue(Numbers.cagr(new double[]{100, 110, 121}) > 0.09);
        assertEquals("12.3%", Numbers.pct(0.123));
    }
}
