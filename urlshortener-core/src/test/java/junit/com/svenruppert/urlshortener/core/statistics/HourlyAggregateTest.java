package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.statistics.HourlyAggregate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HourlyAggregateTest {

  private static final LocalDate D = LocalDate.of(2024, 1, 15);

  @Test
  void newAggregate_isEmpty() {
    HourlyAggregate a = new HourlyAggregate(D);
    assertEquals(D, a.date());
    assertEquals(0, a.totalCount());
    for (int h = 0; h < 24; h++) {
      assertEquals(0, a.getCount(h));
    }
  }

  @Test
  void increment_addsExactlyOneToTheChosenHour() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.increment(0);
    a.increment(0);
    a.increment(23);

    assertEquals(2, a.getCount(0));
    assertEquals(0, a.getCount(1));
    assertEquals(1, a.getCount(23));
    assertEquals(3, a.totalCount());
  }

  @Test
  void add_addsArbitraryValuesAtHour() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.add(5, 17);
    a.add(5, 3);
    a.add(12, 8);

    assertEquals(20, a.getCount(5));
    assertEquals(8, a.getCount(12));
    assertEquals(28, a.totalCount());
  }

  @Test
  void increment_andGet_rejectInvalidHours() {
    HourlyAggregate a = new HourlyAggregate(D);
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> a.increment(-1)),
        () -> assertThrows(IllegalArgumentException.class, () -> a.increment(24)),
        () -> assertThrows(IllegalArgumentException.class, () -> a.add(-1, 1)),
        () -> assertThrows(IllegalArgumentException.class, () -> a.add(24, 1)),
        () -> assertThrows(IllegalArgumentException.class, () -> a.getCount(-1)),
        () -> assertThrows(IllegalArgumentException.class, () -> a.getCount(24))
    );
  }

  @Test
  void peakHour_returnsHourWithHighestCount() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.add(3, 4);
    a.add(7, 9);
    a.add(15, 5);
    assertEquals(7, a.peakHour());
  }

  @Test
  void peakHour_zeroWhenAllZero() {
    HourlyAggregate a = new HourlyAggregate(D);
    assertEquals(0, a.peakHour());
  }

  @Test
  void peakHour_picksFirstOnTies() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.add(5, 3);
    a.add(9, 3);
    assertEquals(5, a.peakHour());
  }

  @Test
  void hourlyCounts_returnsDefensiveCopy() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.increment(1);
    long[] snapshot = a.hourlyCounts();
    snapshot[1] = 999;
    assertEquals(1, a.getCount(1), "internal state must not be affected by mutating the snapshot");
    assertNotSame(snapshot, a.hourlyCounts());
  }

  @Test
  void equals_andHashCode_areConsistent() {
    HourlyAggregate a = new HourlyAggregate(D);
    HourlyAggregate b = new HourlyAggregate(D);
    a.increment(5);
    b.increment(5);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    b.increment(5);
    assertNotEquals(a, b);
  }

  @Test
  void equals_distinguishesDate() {
    HourlyAggregate a = new HourlyAggregate(D);
    HourlyAggregate b = new HourlyAggregate(D.plusDays(1));
    assertNotEquals(a, b);
  }

  @Test
  void equals_handlesNullAndOtherType() {
    HourlyAggregate a = new HourlyAggregate(D);
    assertNotEquals(a, null);
    assertNotEquals(a, "abc");
    assertEquals(a, a);
  }

  @Test
  void toString_containsDateAndCounts() {
    HourlyAggregate a = new HourlyAggregate(D);
    a.increment(7);
    String s = a.toString();
    assertTrue(s.contains(D.toString()));
    assertTrue(s.contains("totalCount=1"));
    assertTrue(s.contains("peakHour=7"));
  }
}
