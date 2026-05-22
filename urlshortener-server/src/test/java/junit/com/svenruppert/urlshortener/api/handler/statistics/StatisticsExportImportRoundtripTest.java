package junit.com.svenruppert.urlshortener.api.handler.statistics;

import com.svenruppert.urlshortener.api.handler.statistics.exports.StatisticsExportWriter;
import com.svenruppert.urlshortener.api.handler.statistics.imports.StatisticsImportReader;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_EXPORT_NDJSON_ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsExportImportRoundtripTest {

  private static final long MAX_NDJSON_BYTES = 64L * 1024 * 1024;

  private Clock fixedClock;
  private InMemoryStatisticsStore source;

  @BeforeEach
  void setUp() {
    Instant fixed = LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC);
    fixedClock = Clock.fixed(fixed, ZoneOffset.UTC);
    source = new InMemoryStatisticsStore(fixedClock);
  }

  @Test
  @DisplayName("export then import yields the same event counts and aggregates")
  void roundtrip_preservesCountsAndAggregates() throws IOException {
    LocalDate d1 = LocalDate.of(2024, 1, 15);
    LocalDate d2 = LocalDate.of(2024, 1, 16);

    record("abc", d1, 9);
    record("abc", d1, 9);
    record("abc", d1, 14);
    record("abc", d2, 8);
    record("xyz", d1, 22);

    long abcTotalBefore = source.getTotalCount("abc");
    long xyzTotalBefore = source.getTotalCount("xyz");
    long abcD1Before = source.getCountForDate("abc", d1);
    long abcHour9Before = source.getHourlyAggregate("abc", d1).orElseThrow().getCount(9);

    byte[] zipBytes = exportToZip(source, d1, d2, Set.of("abc", "xyz"));

    InMemoryStatisticsStore target = new InMemoryStatisticsStore(fixedClock);
    var reader = new StatisticsImportReader(target, MAX_NDJSON_BYTES);
    StatisticsImportReader.Result result = reader.importFrom(new ByteArrayInputStream(zipBytes));

    assertEquals(5, result.importedEvents());
    assertEquals(0, result.skippedLines());
    assertEquals(d1, result.minDate());
    assertEquals(d2, result.maxDate());

    target.reaggregate(result.minDate(), result.maxDate());

    assertEquals(abcTotalBefore, target.getTotalCount("abc"));
    assertEquals(xyzTotalBefore, target.getTotalCount("xyz"));
    assertEquals(abcD1Before, target.getCountForDate("abc", d1));
    assertEquals(abcHour9Before, target.getHourlyAggregate("abc", d1).orElseThrow().getCount(9));
    assertEquals(Set.of("abc", "xyz"), target.getKnownShortCodes());
  }

  @Test
  @DisplayName("re-import into already-populated store doubles counts (append semantics)")
  void appendImport_doublesCountsOnSecondRun() throws IOException {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("abc", d, 10);
    record("abc", d, 11);

    byte[] zipBytes = exportToZip(source, d, d, Set.of("abc"));

    InMemoryStatisticsStore target = new InMemoryStatisticsStore(fixedClock);
    var reader = new StatisticsImportReader(target, MAX_NDJSON_BYTES);

    reader.importFrom(new ByteArrayInputStream(zipBytes));
    target.reaggregate(d, d);
    assertEquals(2, target.getCountForDate("abc", d));

    reader.importFrom(new ByteArrayInputStream(zipBytes));
    target.reaggregate(d, d);
    assertEquals(4, target.getCountForDate("abc", d),
                 "second append must accumulate, then reaggregate sees both batches");
  }

  @Test
  @DisplayName("reaggregate after deleteEventsInRange + import gives clean state")
  void replaceFlow_isIdempotent() throws IOException {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("abc", d, 10);
    record("abc", d, 11);
    record("abc", d, 12);

    byte[] zipBytes = exportToZip(source, d, d, Set.of("abc"));

    // Pretend target had stale data
    InMemoryStatisticsStore target = new InMemoryStatisticsStore(fixedClock);
    record(target, "abc", d, 23);
    record(target, "abc", d, 23);
    assertEquals(2, target.getCountForDate("abc", d));

    // Replace flow: delete in range, import, reaggregate
    long deleted = target.deleteEventsInRange(null, d, d);
    assertTrue(deleted >= 0);

    var reader = new StatisticsImportReader(target, MAX_NDJSON_BYTES);
    reader.importFrom(new ByteArrayInputStream(zipBytes));
    target.reaggregate(d, d);

    assertEquals(3, target.getCountForDate("abc", d),
                 "after replace+import the target reflects only the imported events");
  }

  private void record(String code, LocalDate date, int hour) {
    record(source, code, date, hour);
  }

  private void record(InMemoryStatisticsStore store, String code, LocalDate date, int hour) {
    Instant ts = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0, 0)).toInstant(ZoneOffset.UTC);
    store.recordEvent(RedirectEvent.minimal(code, ts));
  }

  private static byte[] exportToZip(InMemoryStatisticsStore store,
                                    LocalDate from,
                                    LocalDate to,
                                    Set<String> codes)
      throws IOException {
    var writer = new StatisticsExportWriter(store);
    var raw = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(raw)) {
      zip.putNextEntry(new ZipEntry(STATISTICS_EXPORT_NDJSON_ENTRY));
      writer.writeNdjsonTo(zip, Instant.now(), from, to, codes);
      zip.closeEntry();
    }
    return raw.toByteArray();
  }
}
