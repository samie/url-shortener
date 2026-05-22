package junit.com.svenruppert.urlshortener.api.handler.statistics.imports;

import com.svenruppert.urlshortener.api.handler.statistics.imports.StatisticsImportReader;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_EXPORT_NDJSON_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsImportReaderTest {

  private static final long MAX_BYTES = 64L * 1024 * 1024;

  private InMemoryStatisticsStore store;
  private StatisticsImportReader reader;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(
        LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC);
    store = new InMemoryStatisticsStore(clock);
    reader = new StatisticsImportReader(store, MAX_BYTES);
  }

  @Test
  @DisplayName("zip slip guard rejects '..' in entry names")
  void zipSlip_dotDot_isRejected() {
    byte[] zip = makeZip("../evil.ndjson", "{\"type\":\"header\",\"formatVersion\":\"1\"}\n");
    IOException ex = assertThrows(IOException.class,
                                  () -> reader.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("Invalid zip entry name"));
  }

  @Test
  @DisplayName("zip slip guard rejects backslash in entry names")
  void zipSlip_backslash_isRejected() {
    byte[] zip = makeZip("foo\\bar.ndjson", "{\"type\":\"header\",\"formatVersion\":\"1\"}\n");
    IOException ex = assertThrows(IOException.class,
                                  () -> reader.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("Invalid zip entry name"));
  }

  @Test
  @DisplayName("zip slip guard rejects entry names starting with '/'")
  void zipSlip_leadingSlash_isRejected() {
    byte[] zip = makeZip("/abs.ndjson", "{\"type\":\"header\",\"formatVersion\":\"1\"}\n");
    IOException ex = assertThrows(IOException.class,
                                  () -> reader.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("Invalid zip entry name"));
  }

  @Test
  @DisplayName("missing expected entry name throws")
  void missingEntry_throws() {
    byte[] zip = makeZip("unrelated.txt", "irrelevant");
    IOException ex = assertThrows(IOException.class,
                                  () -> reader.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("Expected entry not found"));
  }

  @Test
  @DisplayName("rejects unsupported formatVersion in header")
  void unsupportedFormatVersion_throws() {
    String ndjson = "{\"type\":\"header\",\"formatVersion\":\"999\"}\n";
    byte[] zip = makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson);
    IOException ex = assertThrows(IOException.class,
                                  () -> reader.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("Unsupported formatVersion"));
  }

  @Test
  @DisplayName("skips blank lines without counting them")
  void blankLines_areIgnored() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "\n"
        + "   \n"
        + event("abc", "2024-01-15T10:00:00Z") + "\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(1, r.importedEvents());
    assertEquals(0, r.skippedLines());
  }

  @Test
  @DisplayName("malformed JSON line is counted as skipped, not fatal")
  void malformedLine_isSkipped() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "{not valid json\n"
        + event("abc", "2024-01-15T10:00:00Z") + "\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(1, r.importedEvents());
    assertEquals(1, r.skippedLines());
  }

  @Test
  @DisplayName("event missing shortCode is skipped")
  void event_missingShortCode_isSkipped() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "{\"timestamp\":\"2024-01-15T10:00:00Z\"}\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(0, r.importedEvents());
    assertEquals(1, r.skippedLines());
  }

  @Test
  @DisplayName("event missing timestamp is skipped")
  void event_missingTimestamp_isSkipped() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "{\"shortCode\":\"abc\"}\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(0, r.importedEvents());
    assertEquals(1, r.skippedLines());
  }

  @Test
  @DisplayName("event with unparseable timestamp is skipped")
  void event_invalidTimestamp_isSkipped() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + "{\"shortCode\":\"abc\",\"timestamp\":\"not-a-date\"}\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(0, r.importedEvents());
    assertEquals(1, r.skippedLines());
  }

  @Test
  @DisplayName("min/max dates track the timestamps of imported events")
  void minMaxDates_reflectImportedEvents() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + event("abc", "2024-01-15T10:00:00Z") + "\n"
        + event("abc", "2024-01-13T22:00:00Z") + "\n"
        + event("abc", "2024-01-17T05:00:00Z") + "\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(3, r.importedEvents());
    assertEquals(LocalDate.of(2024, 1, 13), r.minDate());
    assertEquals(LocalDate.of(2024, 1, 17), r.maxDate());
  }

  @Test
  @DisplayName("import without any events leaves min/max null")
  void emptyImport_leavesMinMaxNull() throws IOException {
    String ndjson = "{\"type\":\"header\",\"formatVersion\":\"1\"}\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(0, r.importedEvents());
    assertNull(r.minDate());
    assertNull(r.maxDate());
  }

  @Test
  @DisplayName("first non-header line is treated as an event when no header is present")
  void noHeader_firstLineIsEvent() throws IOException {
    String ndjson = event("abc", "2024-01-15T10:00:00Z") + "\n";
    StatisticsImportReader.Result r = reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertEquals(1, r.importedEvents());
  }

  @Test
  @DisplayName("bounded input stream caps uncompressed bytes")
  void boundedInputStream_capsExcessivePayload() {
    // Produce > 1024 bytes of valid NDJSON but cap reader at 100 bytes
    StringBuilder big = new StringBuilder("{\"type\":\"header\",\"formatVersion\":\"1\"}\n");
    for (int i = 0; i < 200; i++) {
      big.append(event("abc", "2024-01-15T10:00:00Z")).append('\n');
    }
    byte[] zip = makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, big.toString());
    StatisticsImportReader tight = new StatisticsImportReader(store, 100);
    IOException ex = assertThrows(IOException.class,
                                  () -> tight.importFrom(new ByteArrayInputStream(zip)));
    assertTrue(ex.getMessage().contains("exceeds limit"));
  }

  @Test
  @DisplayName("events arrive in the underlying store with shortCode preserved")
  void importedEvents_areVisibleInStore() throws IOException {
    String ndjson = ""
        + "{\"type\":\"header\",\"formatVersion\":\"1\"}\n"
        + event("abc", "2024-01-15T10:00:00Z") + "\n"
        + event("xyz", "2024-01-15T11:00:00Z") + "\n";
    reader.importFrom(new ByteArrayInputStream(makeZip(STATISTICS_EXPORT_NDJSON_ENTRY, ndjson)));
    assertTrue(store.getKnownShortCodes().contains("abc"));
    assertTrue(store.getKnownShortCodes().contains("xyz"));
    assertEquals(2, store.getTotalEventCount());
  }

  private static String event(String code, String ts) {
    return "{\"shortCode\":\"" + code + "\",\"timestamp\":\"" + ts + "\"}";
  }

  private static byte[] makeZip(String entryName, String content) {
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(raw)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return raw.toByteArray();
  }

  // unused: kept to silence the IDE warning if Instant import is later needed
  @SuppressWarnings("unused")
  private static Instant ts(String iso) {
    return Instant.parse(iso);
  }
}
