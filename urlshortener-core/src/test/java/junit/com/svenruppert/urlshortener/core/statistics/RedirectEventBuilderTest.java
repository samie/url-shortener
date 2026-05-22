package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import com.svenruppert.urlshortener.core.statistics.RedirectEventBuilder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedirectEventBuilderTest {

  private static final Instant FIXED = Instant.parse("2024-01-15T12:34:56Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED, ZoneOffset.UTC);

  @Test
  void forShortCode_rejectsNull() {
    assertThrows(NullPointerException.class,
                 () -> RedirectEventBuilder.forShortCode(null));
  }

  @Test
  void build_withClock_usesClockWhenTimestampNotSet() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK).build();
    assertEquals(FIXED, ev.timestamp());
    assertEquals("abc", ev.shortCode());
  }

  @Test
  void build_explicitTimestamp_overridesClock() {
    Instant explicit = Instant.parse("2025-06-01T00:00:00Z");
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .timestamp(explicit)
        .build();
    assertEquals(explicit, ev.timestamp());
  }

  @Test
  void build_nullClock_fallsBackToSystem() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", null).build();
    assertNotNull(ev.timestamp());
  }

  @Test
  void build_capturesAllOptionalHeaders() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .userAgent("ua")
        .referer("ref")
        .ipHash("hash123")
        .acceptLanguage("en-US")
        .build();

    assertEquals("ua", ev.userAgent());
    assertEquals("ref", ev.referer());
    assertEquals("hash123", ev.ipHash());
    assertEquals("en-US", ev.acceptLanguage());
  }

  @Test
  void sanitize_blankOrNullBecomesNull() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .userAgent("")
        .referer("   ")
        .acceptLanguage(null)
        .build();
    assertNull(ev.userAgent());
    assertNull(ev.referer());
    assertNull(ev.acceptLanguage());
  }

  @Test
  void sanitize_trimsLeadingAndTrailingWhitespace() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .userAgent("  Mozilla/5.0   ")
        .build();
    assertEquals("Mozilla/5.0", ev.userAgent());
  }

  @Test
  void sanitize_caps500CharsForLongValues() {
    String longVal = "x".repeat(800);
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .userAgent(longVal)
        .build();
    assertEquals(500, ev.userAgent().length());
  }

  @Test
  void sanitize_preservesValueExactlyAt500() {
    String value = "y".repeat(500);
    RedirectEvent ev = RedirectEventBuilder.forShortCode("abc", FIXED_CLOCK)
        .userAgent(value)
        .build();
    assertEquals(value, ev.userAgent());
  }

  @Test
  void ipAddress_sameInputProducesSameHash() {
    RedirectEvent a = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress("1.2.3.4").build();
    RedirectEvent b = RedirectEventBuilder.forShortCode("y", FIXED_CLOCK)
        .ipAddress("1.2.3.4").build();
    assertNotNull(a.ipHash());
    assertEquals(a.ipHash(), b.ipHash());
  }

  @Test
  void ipAddress_differentInputProducesDifferentHash() {
    RedirectEvent a = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress("1.2.3.4").build();
    RedirectEvent b = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress("1.2.3.5").build();
    assertNotEquals(a.ipHash(), b.ipHash());
  }

  @Test
  void ipAddress_hashLengthIs16HexChars() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress("10.0.0.1").build();
    assertEquals(16, ev.ipHash().length());
  }

  @Test
  void ipAddress_blankOrNullReturnsNullHash() {
    RedirectEvent a = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress(null).build();
    RedirectEvent b = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipAddress("   ").build();
    assertNull(a.ipHash());
    assertNull(b.ipHash());
  }

  @Test
  void ipHash_setsValueDirectlyWithoutHashing() {
    RedirectEvent ev = RedirectEventBuilder.forShortCode("x", FIXED_CLOCK)
        .ipHash("already-hashed").build();
    assertEquals("already-hashed", ev.ipHash());
  }
}
