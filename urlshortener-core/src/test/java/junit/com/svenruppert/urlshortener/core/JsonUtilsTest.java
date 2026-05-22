package junit.com.svenruppert.urlshortener.core;






import com.svenruppert.urlshortener.core.JsonUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

  @Test
  void parseValidJsonString()
      throws IOException {
    String json = "{\"key\":\"value\",\"num\":\"123\"}";
    Map<String, String> result = JsonUtils.parseJson(json);
    assertEquals(2, result.size());
    assertEquals("value", result.get("key"));
    assertEquals("123", result.get("num"));
  }

  @Test
  void parseEmptyJsonStringReturnsEmptyMap()
      throws IOException {
    Map<String, String> result = JsonUtils.parseJson("{}");
    assertTrue(result.isEmpty());
  }

  @Test
  void parseInvalidJsonThrowsIOException() {
    assertThrows(MismatchedInputException.class, () -> JsonUtils.parseJson("\"not json\""));
    assertThrows(StreamReadException.class, () -> JsonUtils.parseJson("{missingColon}"));
  }

  @Test
  void parseJsonFromInputStream()
      throws IOException {
    String json = "{\"a\":\"b\"}";
    var input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    Map<String, String> result = JsonUtils.parseJson(input);
    assertEquals(1, result.size());
    assertEquals("b", result.get("a"));
  }

  @Test
  void toJsonProducesExpectedString() {
    Map<String, String> map = Map.of("foo", "bar", "x", "42");
    String json = JsonUtils.toJson(map);
    assertTrue(json.contains("\"foo\":\"bar\""));
    assertTrue(json.contains("\"x\":\"42\""));
    assertTrue(json.startsWith("{") && json.endsWith("}"));
  }

  @Test
  void toJsonHandlesEscaping() {
    Map<String, String> map = Map.of("key", "value\"", "k\"y", "v");
    String json = JsonUtils.toJson(map);
    assertTrue(json.contains("\\\""));
    assertTrue(json.contains("\"k\\\"y\":\"v\""));
  }

  @Test
  void extractShortCodeFromValidJson()
      throws IOException {
    String json = "{\"shortCode\":\"abc123\"}";
    String code = JsonUtils.extractShortCode(json);
    assertEquals("abc123", code);
  }

  @Test
  void extractShortCodeThrowsIfMissing() {
    String json = "{\"url\":\"https://example.com\"}";
    IOException ex = assertThrows(IOException.class, () -> JsonUtils.extractShortCode(json));
    assertTrue(ex.getMessage().contains("no shortCode"));
  }
}