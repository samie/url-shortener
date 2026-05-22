package junit.com.svenruppert.urlshortener.core;





import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.core.urlmapping.ShortenRequest;
import com.svenruppert.urlshortener.core.urlmapping.ShortenResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsAdvancedTest
    implements HasLogger {

  @Test
  void toJsonArrayOfObjects_handles_nulls_and_order() {
    List<Map<String, String>> items = new ArrayList<>();
    Map<String, String> m1 = new LinkedHashMap<>();
    m1.put("a", "1");
    m1.put("b", null);
    items.add(m1);
    Map<String, String> m2 = new LinkedHashMap<>();
    m2.put("c", "3");
    items.add(m2);

    String json = JsonUtils.toJsonArrayOfObjects(items);
    assertEquals("[{\"a\":\"1\",\"b\":null},{\"c\":\"3\"}]", json);
  }

  @Test
  void toJsonListing_wraps_array_and_metadata() {
    List<Map<String, String>> items = List.of(Map.of("x", "y"));
    String json = JsonUtils.toJsonListing("test", 1, items);
    assertTrue(json.startsWith("{"));
    assertTrue(json.contains("\"mode\":\"test\""));
    assertTrue(json.contains("\"count\":1"));
    assertTrue(json.contains("\"items\":[{"));
    assertTrue(json.endsWith("}"));
  }

  @Nested
  class EscapeUnescape {
    @Test
    void roundTrip_ascii_and_quotes() {
      String original = "simple \"quoted\" \\ text";
      String escaped = JsonUtils.escape(original);
      assertEquals("simple \\\"quoted\\\" \\\\ text", escaped);
      String unescaped = JsonUtils.unescape(escaped);
      assertEquals(original, unescaped);
    }

    @Test
    void escape_controls_and_unicode() {
      String raw = "\b\f\n\r\t" + '€' + " and snowman: " + '\u2603';
      String escaped = JsonUtils.escape(raw);

      assertTrue(escaped.contains("\\b"));
      assertTrue(escaped.contains("\\f"));
      assertTrue(escaped.contains("\\n"));
      assertTrue(escaped.contains("\\r"));
      assertTrue(escaped.contains("\\t"));
      assertTrue(escaped.contains("\\u20ac"));
      assertTrue(escaped.contains("\\u2603"));
      assertEquals(raw, JsonUtils.unescape(escaped));
    }

  }

  @Nested
  class DTOJson {
    @Test
    void toJson_ShortenRequest_and_fromJson_roundtrip() {
      ShortenRequest req = new ShortenRequest("https://example.com?q=1", "alias-01", null, true);
      String json = JsonUtils.toJson(req);

      assertTrue(json.contains("\"url\":\"https://example.com?q=1\""));
      assertTrue(json.contains("\"shortURL\":\"alias-01\""));

      ShortenRequest parsed = JsonUtils.fromJson(json, ShortenRequest.class);
      assertEquals(req.getUrl(), parsed.getUrl());
      assertEquals(req.getShortURL(), parsed.getShortURL());
    }


    @Test
    void fromJson_invalid_missing_url_throws() {
      String invalid = "{\"alias\":\"x\"}";
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                 () -> JsonUtils.fromJson(invalid, ShortenRequest.class));
      assertTrue(ex.getMessage().toLowerCase().contains("url"));
    }

    @Test
    void toJson_ShortenResponse_and_ShortUrlMapping() {
      ShortenResponse res = new ShortenResponse("sc", "https://e");
      String resJson = JsonUtils.toJson(res);
      logger().info("toJson_ShortenResponse_and_ShortUrlMapping - resJson - {}", resJson);
      assertEquals("{\"shortCode\":\"sc\",\"originalUrl\":\"https://e\"}", resJson);



      ShortUrlMapping map = new ShortUrlMapping(
          "sc",
          "https://e",
          Instant.parse("2020-01-01T00:00:00Z"),
          null,
          true
      );

      String mapJson = JsonUtils.toJson(map);
      logger().info("toJson_ShortenResponse_and_ShortUrlMapping - mapJson - {}", mapJson);
      assertTrue(mapJson.contains("\"shortCode\":\"sc\""));
      assertTrue(mapJson.contains("\"originalUrl\":\"https://e\""));

      // alias fällt weg (Option 1)
      assertFalse(mapJson.contains("\"shortURL\""));

      // createdAt nur prüfen, wenn es wirklich als JSON-Feld existieren soll
      assertTrue(mapJson.contains("\"createdAt\":\"2020-01-01T00:00:00Z\""));
    }

  }
}