package junit.com.svenruppert.urlshortener.api.provider.inmemory;

import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Targeted unit tests for the recently added methods on
 * {@link InMemoryStatisticsStore}: {@code getKnownShortCodes},
 * {@code deleteEventsInRange} and {@code reaggregate}.
 */
class InMemoryStatisticsStoreExtraTest {

  private InMemoryStatisticsStore store;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(
        LocalDateTime.of(2024, 1, 20, 12, 0, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC);
    store = new InMemoryStatisticsStore(fixedClock);
  }

  private void record(String code, LocalDate date, int hour) {
    store.recordEvent(RedirectEvent.minimal(code,
        LocalDateTime.of(date, LocalTime.of(hour, 0)).toInstant(ZoneOffset.UTC)));
  }

  // -------- getKnownShortCodes --------

  @Test
  @DisplayName("empty store reports no known short codes")
  void getKnownShortCodes_emptyStore() {
    assertTrue(store.getKnownShortCodes().isEmpty());
  }

  @Test
  @DisplayName("known short codes union events, hourly and daily")
  void getKnownShortCodes_unionAcrossThreeMaps() {
    record("a", LocalDate.of(2024, 1, 15), 9);
    record("b", LocalDate.of(2024, 1, 16), 9);
    record("c", LocalDate.of(2024, 1, 17), 9);

    var codes = store.getKnownShortCodes();
    assertEquals(3, codes.size());
    assertTrue(codes.contains("a"));
    assertTrue(codes.contains("b"));
    assertTrue(codes.contains("c"));
  }

  // -------- deleteEventsInRange --------

  @Test
  @DisplayName("delete range removes events for a single code and leaves others untouched")
  void deleteEventsInRange_singleCode_isolatedFromOthers() {
    LocalDate d1 = LocalDate.of(2024, 1, 15);
    LocalDate d2 = LocalDate.of(2024, 1, 16);
    record("a", d1, 9);
    record("a", d2, 9);
    record("b", d1, 9);

    long deleted = store.deleteEventsInRange("a", d1, d2);
    assertEquals(2, deleted);

    assertTrue(store.getEventsForDate("a", d1).isEmpty());
    assertTrue(store.getEventsForDate("a", d2).isEmpty());
    assertFalse(store.getEventsForDate("b", d1).isEmpty(),
                "other code must stay intact");
  }

  @Test
  @DisplayName("delete range with null code applies to all known codes")
  void deleteEventsInRange_nullCode_appliesGlobally() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("a", d, 9);
    record("b", d, 9);
    record("c", d, 9);

    long deleted = store.deleteEventsInRange(null, d, d);
    assertEquals(3, deleted);
    // Per Javadoc deleteEventsInRange touches only raw events; aggregates remain
    // until reaggregate(...) is invoked. Verify events are gone:
    assertTrue(store.getEventsForDate("a", d).isEmpty());
    assertTrue(store.getEventsForDate("b", d).isEmpty());
    assertTrue(store.getEventsForDate("c", d).isEmpty());
  }

  @Test
  @DisplayName("delete range with from>to is a no-op")
  void deleteEventsInRange_invertedRange_isNoOp() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("a", d, 9);

    long deleted = store.deleteEventsInRange("a", d.plusDays(1), d);
    assertEquals(0, deleted);
    assertEquals(1, store.getEventsForDate("a", d).size());
  }

  @Test
  @DisplayName("delete range covers only the bounded days")
  void deleteEventsInRange_respectsBounds() {
    LocalDate d1 = LocalDate.of(2024, 1, 14);
    LocalDate d2 = LocalDate.of(2024, 1, 15);
    LocalDate d3 = LocalDate.of(2024, 1, 16);
    record("a", d1, 9);
    record("a", d2, 9);
    record("a", d3, 9);

    long deleted = store.deleteEventsInRange("a", d2, d2);
    assertEquals(1, deleted);
    assertEquals(1, store.getEventsForDate("a", d1).size());
    assertTrue(store.getEventsForDate("a", d2).isEmpty());
    assertEquals(1, store.getEventsForDate("a", d3).size());
  }

  @Test
  @DisplayName("delete range on unknown code returns zero")
  void deleteEventsInRange_unknownCode_returnsZero() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    long deleted = store.deleteEventsInRange("nope", d, d);
    assertEquals(0, deleted);
  }

  // -------- reaggregate --------

  @Test
  @DisplayName("reaggregate from>to returns zero")
  void reaggregate_invertedRange_returnsZero() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("a", d, 9);
    long buckets = store.reaggregate(d.plusDays(1), d);
    assertEquals(0, buckets);
  }

  @Test
  @DisplayName("reaggregate rebuilds counts after manipulated aggregate state")
  void reaggregate_rebuildsConsistentCounts() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("a", d, 9);
    record("a", d, 9);
    record("a", d, 14);

    // Verify pre-state from recordEvent
    assertEquals(3, store.getCountForDate("a", d));

    // Manually skew the daily aggregate to simulate inconsistency
    store.getDailyAggregate("a", d).orElseThrow().add(99);
    assertEquals(102, store.getCountForDate("a", d), "skew applied");

    long buckets = store.reaggregate(d, d);
    assertEquals(1, buckets);
    assertEquals(3, store.getCountForDate("a", d));
    assertEquals(2, store.getHourlyAggregate("a", d).orElseThrow().getCount(9));
    assertEquals(1, store.getHourlyAggregate("a", d).orElseThrow().getCount(14));
  }

  @Test
  @DisplayName("reaggregate over a day without events removes any stale aggregate")
  void reaggregate_emptyDay_removesStaleAggregate() {
    LocalDate d = LocalDate.of(2024, 1, 15);
    record("a", d, 9);
    store.deleteEventsInRange("a", d, d);
    // Aggregate is still there from before deletion
    assertTrue(store.getDailyAggregate("a", d).isPresent());

    long buckets = store.reaggregate(d, d);
    assertEquals(0, buckets, "no buckets to rebuild because events are gone");
    assertTrue(store.getDailyAggregate("a", d).isEmpty(),
               "stale aggregate must be cleared");
  }

  @Test
  @DisplayName("reaggregate processes each day in the range independently")
  void reaggregate_multiDayRange() {
    LocalDate d1 = LocalDate.of(2024, 1, 15);
    LocalDate d2 = LocalDate.of(2024, 1, 16);
    record("a", d1, 10);
    record("a", d2, 11);
    record("a", d2, 11);

    long buckets = store.reaggregate(d1, d2);
    assertEquals(2, buckets);
    assertEquals(1, store.getCountForDate("a", d1));
    assertEquals(2, store.getCountForDate("a", d2));
  }
}
