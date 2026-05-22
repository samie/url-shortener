package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectEventTest {

  private static final Instant TS = Instant.parse("2024-01-15T10:00:00Z");

  @Test
  void constructor_rejectsNullShortCode() {
    assertThrows(NullPointerException.class,
                 () -> new RedirectEvent(null, TS, null, null, null, null));
  }

  @Test
  void constructor_rejectsNullTimestamp() {
    assertThrows(NullPointerException.class,
                 () -> new RedirectEvent("abc", null, null, null, null, null));
  }

  @Test
  void minimal_buildsEventWithOnlyRequiredFields() {
    RedirectEvent ev = RedirectEvent.minimal("abc", TS);
    assertAll(
        () -> assertEquals("abc", ev.shortCode()),
        () -> assertEquals(TS, ev.timestamp()),
        () -> assertNull(ev.userAgent()),
        () -> assertNull(ev.referer()),
        () -> assertNull(ev.ipHash()),
        () -> assertNull(ev.acceptLanguage())
    );
  }

  @Test
  void allAccessors_returnConstructorArguments() {
    RedirectEvent ev = new RedirectEvent("abc", TS, "ua", "ref", "hash", "en");
    assertEquals("ua", ev.userAgent());
    assertEquals("ref", ev.referer());
    assertEquals("hash", ev.ipHash());
    assertEquals("en", ev.acceptLanguage());
  }

  @Test
  void equals_andHashCode_consistentForIdenticalState() {
    RedirectEvent a = new RedirectEvent("abc", TS, "ua", "ref", "hash", "en");
    RedirectEvent b = new RedirectEvent("abc", TS, "ua", "ref", "hash", "en");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
  }

  @Test
  void equals_distinguishesEachField() {
    RedirectEvent base = new RedirectEvent("abc", TS, "ua", "ref", "hash", "en");
    assertAll(
        () -> assertNotEquals(base, null),
        () -> assertNotEquals(base, "x"),
        () -> assertNotEquals(base, new RedirectEvent("xyz", TS, "ua", "ref", "hash", "en")),
        () -> assertNotEquals(base, new RedirectEvent("abc", TS.plusSeconds(1), "ua", "ref", "hash", "en")),
        () -> assertNotEquals(base, new RedirectEvent("abc", TS, "OTHER", "ref", "hash", "en")),
        () -> assertNotEquals(base, new RedirectEvent("abc", TS, "ua", "OTHER", "hash", "en")),
        () -> assertNotEquals(base, new RedirectEvent("abc", TS, "ua", "ref", "OTHER", "en")),
        () -> assertNotEquals(base, new RedirectEvent("abc", TS, "ua", "ref", "hash", "de"))
    );
  }

  @Test
  void toString_includesShortCodeAndTimestamp() {
    RedirectEvent ev = new RedirectEvent("abc", TS, "ua", null, null, null);
    String s = ev.toString();
    assertTrue(s.contains("abc"));
    assertTrue(s.contains(TS.toString()));
  }
}
