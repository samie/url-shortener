package junit.com.svenruppert.urlshortener.api.handler.statistics.exports;

import com.svenruppert.urlshortener.api.handler.statistics.exports.StatisticsExportWriter;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import com.svenruppert.urlshortener.core.JacksonJson;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_EXPORT_FORMAT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsExportWriterTest {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP_REF =
      new TypeReference<>() { };

  private InMemoryStatisticsStore store;
  private StatisticsExportWriter writer;
  private Instant exportedAt;

  @BeforeEach
  void setUp() {
    Instant now = LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC);
    store = new InMemoryStatisticsStore(Clock.fixed(now, ZoneOffset.UTC));
    writer = new StatisticsExportWriter(store);
    exportedAt = now;
  }

  @Test
  @DisplayName("empty store produces only the header line")
  void emptyStore_writesOnlyHeader() throws IOException {
    var out = new ByteArrayOutputStream();
    var result = writer.writeNdjsonTo(out, exportedAt, null, null, Set.of());

    List<String> lines = lines(out);
    assertEquals(1, lines.size());

    Map<String, Object> header = JacksonJson.mapper().readValue(lines.get(0), MAP_REF);
    assertEquals("header", header.get("type"));
    assertEquals(STATISTICS_EXPORT_FORMAT_VERSION, header.get("formatVersion"));
    assertEquals(exportedAt.toString(), header.get("exportedAt"));
    assertEquals(0, result.eventCount());
  }

  @Test
  @DisplayName("writes one JSON line per event with full payload")
  void writesEventsOneLineEach() throws IOException {
    Instant ts1 = LocalDateTime.of(2024, 1, 15, 10, 0, 0).toInstant(ZoneOffset.UTC);
    Instant ts2 = LocalDateTime.of(2024, 1, 15, 11, 0, 0).toInstant(ZoneOffset.UTC);
    store.recordEvent(new RedirectEvent("abc", ts1, "ua-x", "ref-y", "hash-z", "en"));
    store.recordEvent(RedirectEvent.minimal("abc", ts2));

    var out = new ByteArrayOutputStream();
    var result = writer.writeNdjsonTo(out,
                                      exportedAt,
                                      LocalDate.of(2024, 1, 15),
                                      LocalDate.of(2024, 1, 15),
                                      Set.of("abc"));

    List<String> lines = lines(out);
    assertEquals(3, lines.size(), "header + 2 events");
    assertEquals(2, result.eventCount());

    Map<String, Object> first = JacksonJson.mapper().readValue(lines.get(1), MAP_REF);
    assertEquals("abc", first.get("shortCode"));
    assertEquals(ts1.toString(), first.get("timestamp"));
    assertEquals("ua-x", first.get("userAgent"));
    assertEquals("ref-y", first.get("referer"));
    assertEquals("hash-z", first.get("ipHash"));
    assertEquals("en", first.get("acceptLanguage"));

    Map<String, Object> second = JacksonJson.mapper().readValue(lines.get(2), MAP_REF);
    assertEquals("abc", second.get("shortCode"));
    assertEquals(ts2.toString(), second.get("timestamp"));
    // Minimal event — optional fields must be omitted, not serialized as null
    assertTrue(!second.containsKey("userAgent"));
    assertTrue(!second.containsKey("ipHash"));
  }

  @Test
  @DisplayName("from/to filter excludes events outside the range")
  void filtersByDateRange() throws IOException {
    store.recordEvent(RedirectEvent.minimal("abc",
        LocalDateTime.of(2024, 1, 14, 9, 0, 0).toInstant(ZoneOffset.UTC)));
    store.recordEvent(RedirectEvent.minimal("abc",
        LocalDateTime.of(2024, 1, 15, 9, 0, 0).toInstant(ZoneOffset.UTC)));
    store.recordEvent(RedirectEvent.minimal("abc",
        LocalDateTime.of(2024, 1, 16, 9, 0, 0).toInstant(ZoneOffset.UTC)));

    var out = new ByteArrayOutputStream();
    var result = writer.writeNdjsonTo(out,
                                      exportedAt,
                                      LocalDate.of(2024, 1, 15),
                                      LocalDate.of(2024, 1, 15),
                                      Set.of("abc"));

    assertEquals(1, result.eventCount());
    assertEquals(2, lines(out).size());
  }

  @Test
  @DisplayName("shortCodes filter restricts the export to selected codes")
  void filtersByShortCodes() throws IOException {
    Instant ts = LocalDateTime.of(2024, 1, 15, 9, 0, 0).toInstant(ZoneOffset.UTC);
    store.recordEvent(RedirectEvent.minimal("a", ts));
    store.recordEvent(RedirectEvent.minimal("b", ts));
    store.recordEvent(RedirectEvent.minimal("c", ts));

    var out = new ByteArrayOutputStream();
    var result = writer.writeNdjsonTo(out, exportedAt, null, null, Set.of("a", "c"));

    assertEquals(2, result.eventCount());

    Map<String, Object> header = JacksonJson.mapper().readValue(lines(out).get(0), MAP_REF);
    @SuppressWarnings("unchecked")
    List<String> codes = (List<String>) header.get("shortCodes");
    assertEquals(Set.of("a", "c"), Set.copyOf(codes));
  }

  @Test
  @DisplayName("empty shortCodes parameter exports all known codes")
  void emptyShortCodesExportsAll() throws IOException {
    Instant ts = LocalDateTime.of(2024, 1, 15, 9, 0, 0).toInstant(ZoneOffset.UTC);
    store.recordEvent(RedirectEvent.minimal("a", ts));
    store.recordEvent(RedirectEvent.minimal("b", ts));

    var out = new ByteArrayOutputStream();
    var result = writer.writeNdjsonTo(out, exportedAt, null, null, Set.of());

    assertEquals(2, result.eventCount());
    assertNotNull(result.from());
    assertNotNull(result.to());
  }

  private static List<String> lines(ByteArrayOutputStream out) {
    return List.of(out.toString(StandardCharsets.UTF_8).split("\n"));
  }
}
