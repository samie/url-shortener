package junit.com.svenruppert.urlshortener.core;





import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.urlmapping.ShortenRequest;
import org.junit.jupiter.api.Test;

import static com.svenruppert.urlshortener.core.JsonUtils.fromJson;
import static com.svenruppert.urlshortener.core.JsonUtils.toJson;
import static org.junit.jupiter.api.Assertions.*;

class ShortenRequestJsonTest {

  @Test
  void test001() {
    ShortenRequest req = new ShortenRequest("https://example.com/path?q=\"x\"", null, null,true);
    var json = toJson(req);
    var fromJson = fromJson(json, ShortenRequest.class);
    assertEquals(req, fromJson);
  }

  @Test
  void toJson_with_null_alias_writes_null() {
    ShortenRequest req = new ShortenRequest("https://example.com/path?q=\"x\"", null, null,true);
    String json = toJson(req);
    System.out.println("json = " + json);
    assertTrue(json.contains("\"url\":\"https://example.com/path?q=\\\"x\\\"\""));
    assertTrue(json.contains("\"shortURL\":null"));
  }

  @Test
  void toJson_with_alias_writes_string_and_escapes() {
    ShortenRequest req = new ShortenRequest("https://example.com", "a\tb\nc\"d",null, true);
    String json = toJson(req);
    System.out.println("json = " + json);
    assertTrue(json.contains("\"url\":\"https://example.com\""));
    // alias should be quoted and contain escapes
    assertTrue(json.contains("\"shortURL\":\"a\\tb\\nc\\\"d\""));
  }

  @Test
  void fromJson_accepts_null_alias_and_reads_url() {
    String json = "{\n  \"url\": \"https://e\",\n  \"alias\": null\n}";
    ShortenRequest req = JsonUtils.fromJson(json, ShortenRequest.class);
    assertEquals("https://e", req.getUrl());
    assertFalse(req.hasAlias());
    assertNull(req.getShortURL());
  }

  @Test
  void fromJson_rejects_non_object() {
    String json = "[]";
    assertThrows(IllegalArgumentException.class, () -> JsonUtils.fromJson(json, ShortenRequest.class));
  }
}