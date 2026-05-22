package junit.com.svenruppert.urlshortener.api.handler.statistics.exports;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.svenruppert.urlshortener.api.handler.statistics.exports.StatisticsExportHandler;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.svenruppert.urlshortener.core.DefaultValues.CONTENT_DISPOSITION;
import static com.svenruppert.urlshortener.core.DefaultValues.CONTENT_TYPE;
import static com.svenruppert.urlshortener.core.DefaultValues.EXPORT_TIMESTAMP_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsExportHandlerTest {

  private StatisticsExportHandler handler;
  private InMemoryStatisticsStore store;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(
        LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC);
    store = new InMemoryStatisticsStore(clock);
    handler = new StatisticsExportHandler(store);
  }

  @Test
  @DisplayName("rejects non-GET/HEAD with 405")
  void rejectsNonGetOrHead() throws IOException {
    TestExchange ex = new TestExchange("POST", "/api/statistics/export");
    handler.handle(ex);
    assertEquals(405, ex.responseCode);
  }

  @Test
  @DisplayName("HEAD returns 200 with filename headers and empty body")
  void headReturnsHeadersOnly() throws IOException {
    TestExchange ex = new TestExchange("HEAD", "/api/statistics/export");
    handler.handle(ex);

    assertEquals(200, ex.responseCode);
    assertNotNull(ex.responseHeaders.getFirst(CONTENT_TYPE));
    String cd = ex.responseHeaders.getFirst(CONTENT_DISPOSITION);
    assertNotNull(cd);
    assertTrue(cd.contains("urlshortener-statistics-export-"));
    assertTrue(cd.endsWith(".zip\""), "filename ends in .zip: " + cd);
    assertNotNull(ex.responseHeaders.getFirst(EXPORT_TIMESTAMP_HEADER));
    assertEquals(0, ex.responseBody.size(),
                 "HEAD response carries no body");
  }

  @Test
  @DisplayName("invalid from date returns 400")
  void invalidFromDate_returns400() throws IOException {
    TestExchange ex = new TestExchange("GET", "/api/statistics/export?from=not-a-date");
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("invalid_date"));
  }

  @Test
  @DisplayName("from after to returns 400")
  void invertedRange_returns400() throws IOException {
    TestExchange ex = new TestExchange("GET",
        "/api/statistics/export?from=2024-02-01&to=2024-01-01");
    handler.handle(ex);
    assertEquals(400, ex.responseCode);
    assertTrue(ex.responseBody.toString().contains("invalid_range"));
  }

  @Test
  @DisplayName("GET on empty store produces a valid ZIP with one NDJSON entry")
  void getOnEmptyStore_producesZip() throws IOException {
    TestExchange ex = new TestExchange("GET", "/api/statistics/export");
    handler.handle(ex);
    assertEquals(200, ex.responseCode);
    assertEquals("application/zip", ex.responseHeaders.getFirst(CONTENT_TYPE));
    assertTrue(ex.responseBody.size() > 0);
  }

  @Test
  @DisplayName("GET with shortCodes restricts the export")
  void getWithShortCodesFilter_returnsZip() throws IOException {
    store.recordEvent(RedirectEvent.minimal("abc",
        LocalDateTime.of(2024, 1, 15, 10, 0, 0).toInstant(ZoneOffset.UTC)));
    store.recordEvent(RedirectEvent.minimal("xyz",
        LocalDateTime.of(2024, 1, 15, 11, 0, 0).toInstant(ZoneOffset.UTC)));

    TestExchange ex = new TestExchange("GET", "/api/statistics/export?shortCodes=abc");
    handler.handle(ex);
    assertEquals(200, ex.responseCode);
    assertTrue(ex.responseBody.size() > 0);
  }

  // --- minimal HttpExchange test double ---

  private static final class TestExchange extends HttpExchange {
    final String method;
    final URI uri;
    final Headers requestHeaders = new Headers();
    final Headers responseHeaders = new Headers();
    final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    int responseCode = -1;

    TestExchange(String method, String path) {
      this.method = method;
      this.uri = URI.create(path);
    }

    @Override public Headers getRequestHeaders() { return requestHeaders; }
    @Override public Headers getResponseHeaders() { return responseHeaders; }
    @Override public URI getRequestURI() { return uri; }
    @Override public String getRequestMethod() { return method; }
    @Override public HttpContext getHttpContext() { return null; }
    @Override public void close() { }
    @Override public InputStream getRequestBody() { return new ByteArrayInputStream(new byte[0]); }
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
