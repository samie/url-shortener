package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.statistics.StatisticsImportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsImportResponseTest {

  private static final LocalDate FROM = LocalDate.of(2024, 1, 15);
  private static final LocalDate TO = LocalDate.of(2024, 1, 20);

  @Test
  void constructor_setsAllFields() {
    StatisticsImportResponse r = new StatisticsImportResponse("append", 42, 3, FROM, TO);

    assertAll(
        () -> assertEquals("append", r.getMode()),
        () -> assertEquals("append", r.mode()),
        () -> assertEquals(42L, r.getImportedEvents()),
        () -> assertEquals(42L, r.importedEvents()),
        () -> assertEquals(3L, r.getSkippedLines()),
        () -> assertEquals(3L, r.skippedLines()),
        () -> assertEquals(FROM, r.getFrom()),
        () -> assertEquals(FROM, r.from()),
        () -> assertEquals(TO, r.getTo()),
        () -> assertEquals(TO, r.to())
    );
  }

  @Test
  void defaultConstructor_initializesNullsAndZeros() {
    StatisticsImportResponse r = new StatisticsImportResponse();

    assertAll(
        () -> assertNull(r.mode()),
        () -> assertEquals(0L, r.importedEvents()),
        () -> assertEquals(0L, r.skippedLines()),
        () -> assertNull(r.from()),
        () -> assertNull(r.to())
    );
  }

  @Test
  void setters_updateFields() {
    StatisticsImportResponse r = new StatisticsImportResponse();
    r.setMode("replace");
    r.setImportedEvents(7);
    r.setSkippedLines(1);
    r.setFrom(FROM);
    r.setTo(TO);

    assertEquals("replace", r.mode());
    assertEquals(7L, r.importedEvents());
    assertEquals(1L, r.skippedLines());
    assertEquals(FROM, r.from());
    assertEquals(TO, r.to());
  }

  @Test
  void equals_andHashCode_areConsistentForIdenticalState() {
    StatisticsImportResponse a = new StatisticsImportResponse("append", 5, 0, FROM, TO);
    StatisticsImportResponse b = new StatisticsImportResponse("append", 5, 0, FROM, TO);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
  }

  @Test
  void equals_distinguishesEachField() {
    StatisticsImportResponse base = new StatisticsImportResponse("append", 5, 0, FROM, TO);

    assertAll(
        () -> assertNotEquals(base, null),
        () -> assertNotEquals(base, "string"),
        () -> assertNotEquals(base, new StatisticsImportResponse("replace", 5, 0, FROM, TO)),
        () -> assertNotEquals(base, new StatisticsImportResponse("append", 6, 0, FROM, TO)),
        () -> assertNotEquals(base, new StatisticsImportResponse("append", 5, 1, FROM, TO)),
        () -> assertNotEquals(base, new StatisticsImportResponse("append", 5, 0, FROM.minusDays(1), TO)),
        () -> assertNotEquals(base, new StatisticsImportResponse("append", 5, 0, FROM, TO.plusDays(1)))
    );
  }

  @Test
  void toString_containsKeyFields() {
    String s = new StatisticsImportResponse("append", 5, 0, FROM, TO).toString();
    assertTrue(s.contains("append"));
    assertTrue(s.contains("5"));
    assertTrue(s.contains(FROM.toString()));
    assertTrue(s.contains(TO.toString()));
  }

  @Test
  void roundtrips_viaJackson() {
    StatisticsImportResponse before = new StatisticsImportResponse("replace", 100, 2, FROM, TO);
    String json = JsonUtils.toJson(before);
    StatisticsImportResponse after = JsonUtils.fromJson(json, StatisticsImportResponse.class);
    assertEquals(before, after);
  }
}
