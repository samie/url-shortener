package junit.com.svenruppert.urlshortener.api.handler.statistics.imports;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.svenruppert.urlshortener.api.handler.statistics.imports.StatisticsImportHandler;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_EXPORT_NDJSON_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsImportHandlerTest {

  private StatisticsImportHandler handler;
  private InMemoryStatisticsStore store;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(
        LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC);
    store = new InMemoryStatisticsStore(clock);
    handler = new StatisticsImportHandler(store);
  }

  @Test
  @DisplayName("rejects non-POST with 405")
  void rejectsNonPost() throws IOException {
    TestExchange ex = new TestExchange("GET", "/api/statistics/import", new byte[0]);
    handler.handle(ex);
    assertEquals(405, ex.responseCode);
  }

  @Test
  @DisplayName("replace mode without from/to returns 400")
  void replaceWithoutDates_returns400() throws IOException {
    TestExchange ex = new TestExchange("POST", "/api/statistics/import?mode=replace", validNdjsonZip());
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("missing_parameter"));
  }

  @Test
  @DisplayName("replace mode with only from returns 400")
  void replaceMissingTo_returns400() throws IOException {
    TestExchange ex = new TestExchange("POST",
        "/api/statistics/import?mode=replace&from=2024-01-01", validNdjsonZip());
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
  }

  @Test
  @DisplayName("replace mode with invalid date returns 400")
  void replaceInvalidDate_returns400() throws IOException {
    TestExchange ex = new TestExchange("POST",
        "/api/statistics/import?mode=replace&from=not-a-date&to=2024-01-15",
        validNdjsonZip());
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("invalid_date"));
  }

  @Test
  @DisplayName("replace mode with from after to returns 400")
  void replaceInvertedRange_returns400() throws IOException {
    TestExchange ex = new TestExchange("POST",
        "/api/statistics/import?mode=replace&from=2024-02-01&to=2024-01-01",
        validNdjsonZip());
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("invalid_range"));
  }

  @Test
  @DisplayName("malformed ZIP body produces 400")
  void malformedBody_returns400() throws IOException {
    TestExchange ex = new TestExchange("POST", "/api/statistics/import",
        "not a zip".getBytes(StandardCharsets.UTF_8));
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
  }

  @Test
  @DisplayName("valid append import returns 200 and JSON response")
  void appendImport_returns200() throws IOException {
    TestExchange ex = new TestExchange("POST", "/api/statistics/import", validNdjsonZip());
    handler.handle(ex);
    assertEquals(200, ex.responseCode);
    String body = ex.responseBody.toString();
    assertTrue(body.contains("\"mode\""));
    assertTrue(body.contains("append"));
    assertTrue(body.contains("\"importedEvents\""));
  }

  @Test
  @DisplayName("replace import deletes existing data in the range before importing")
  void replaceImport_returns200() throws IOException {
    String qs = "mode=replace&from=2024-01-15&to=2024-01-15";
    TestExchange ex = new TestExchange("POST", "/api/statistics/import?" + qs, validNdjsonZip());
    handler.handle(ex);
    assertEquals(200, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("replace"));
  }

  private static byte[] validNdjsonZip() {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "{\"shortCode\":\"abc\",\"timestamp\":\"2024-01-15T10:00:00Z\"}\n";
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(raw)) {
      zip.putNextEntry(new ZipEntry(STATISTICS_EXPORT_NDJSON_ENTRY));
      zip.write(ndjson.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return raw.toByteArray();
  }

  private static final class TestExchange extends HttpExchange {
    final String method;
    final URI uri;
    final byte[] body;
    final Headers requestHeaders = new Headers();
    final Headers responseHeaders = new Headers();
    final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    int responseCode = -1;

    TestExchange(String method, String path, byte[] body) {
      this.method = method;
      this.uri = URI.create(path);
      this.body = body;
    }

    @Override public Headers getRequestHeaders() { return requestHeaders; }
    @Override public Headers getResponseHeaders() { return responseHeaders; }
    @Override public URI getRequestURI() { return uri; }
    @Override public String getRequestMethod() { return method; }
    @Override public HttpContext getHttpContext() { return null; }
    @Override public void close() { }
    @Override public InputStream getRequestBody() { return new ByteArrayInputStream(body); }
    @Override public OutputStream getResponseBody() { return responseBody; }
    @Override public void sendResponseHeaders(int rCode, long responseLength) {
      this.responseCode = rCode;
    }
    @Override public InetSocketAddress getRemoteAddress() {
      return new InetSocketAddress("127.0.0.1", 12345);
    }
    @Override public int getResponseCode() { return responseCode; }
    @Override public InetSocketAddress getLocalAddress() {
      return new InetSocketAddress("127.0.0.1", 8080);
    }
    @Override public String getProtocol() { return "HTTP/1.1"; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public void setAttribute(String name, Object value) { }
    @Override public void setStreams(InputStream i, OutputStream o) { }
    @Override public HttpPrincipal getPrincipal() { return null; }
  }
}
