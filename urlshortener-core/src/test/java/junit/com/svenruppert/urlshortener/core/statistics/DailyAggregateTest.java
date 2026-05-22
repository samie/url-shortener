package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.statistics.DailyAggregate;
import com.svenruppert.urlshortener.core.statistics.HourlyAggregate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyAggregateTest {

  private static final LocalDate D = LocalDate.of(2024, 1, 15);

  @Test
  void defaultConstructor_initializesToZero() {
    DailyAggregate a = new DailyAggregate(D);
    assertEquals(D, a.date());
    assertEquals(0L, a.totalCount());
    assertEquals(0L, a.uniqueVisitors());
  }

  @Test
  void valueConstructor_setsBothCounts() {
    DailyAggregate a = new DailyAggregate(D, 42, 7);
    assertEquals(42L, a.totalCount());
    assertEquals(7L, a.uniqueVisitors());
  }

  @Test
  void increment_incrementsTotalByOne() {
    DailyAggregate a = new DailyAggregate(D);
    a.increment();
    a.increment();
    a.increment();
    assertEquals(3L, a.totalCount());
  }

  @Test
  void add_addsExactValue() {
    DailyAggregate a = new DailyAggregate(D);
    a.add(10);
    a.add(7);
    assertEquals(17L, a.totalCount());
  }

  @Test
  void setUniqueVisitors_replacesValue() {
    DailyAggregate a = new DailyAggregate(D);
    a.setUniqueVisitors(5);
    assertEquals(5L, a.uniqueVisitors());
    a.setUniqueVisitors(11);
    assertEquals(11L, a.uniqueVisitors());
  }

  @Test
  void fromHourlyAggregate_copiesTotalCountAndDate() {
    HourlyAggregate h = new HourlyAggregate(D);
    h.add(10, 4);
    h.add(15, 3);

    DailyAggregate d = DailyAggregate.fromHourlyAggregate(h);

    assertEquals(D, d.date());
    assertEquals(7L, d.totalCount());
    assertEquals(0L, d.uniqueVisitors());
  }

  @Test
  void equals_andHashCode_consistentForSameState() {
    DailyAggregate a = new DailyAggregate(D, 5, 2);
    DailyAggregate b = new DailyAggregate(D, 5, 2);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
  }

  @Test
  void equals_distinguishesEachField() {
    DailyAggregate base = new DailyAggregate(D, 5, 2);
    assertNotEquals(base, null);
    assertNotEquals(base, "x");
    assertNotEquals(base, new DailyAggregate(D.plusDays(1), 5, 2));
    assertNotEquals(base, new DailyAggregate(D, 6, 2));
    assertNotEquals(base, new DailyAggregate(D, 5, 3));
  }

  @Test
  void toString_containsKeyFields() {
    String s = new DailyAggregate(D, 5, 2).toString();
    assertTrue(s.contains(D.toString()));
    assertTrue(s.contains("totalCount=5"));
    assertTrue(s.contains("uniqueVisitors=2"));
  }
}
