package com.svenruppert.urlshortener.core.statistics;





import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single redirect event when a short URL is resolved to its target URL.
 * This class captures the timestamp and request metadata for analytics purposes.
 * Designed to be stored permanently in EclipseStore for historical analysis.
 */
public final class RedirectEvent implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String shortCode;
  private final Instant timestamp;
  private final String userAgent;
  private final String referer;
  private final String ipHash;
  private final String acceptLanguage;

  public RedirectEvent(
      String shortCode,
      Instant timestamp,
      String userAgent,
      String referer,
      String ipHash,
      String acceptLanguage
  ) {
    this.shortCode = Objects.requireNonNull(shortCode, "shortCode must not be null");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    this.userAgent = userAgent;
    this.referer = referer;
    this.ipHash = ipHash;
    this.acceptLanguage = acceptLanguage;
  }

  /**
   * Creates a minimal event with only the required fields.
   */
  public static RedirectEvent minimal(String shortCode, Instant timestamp) {
    return new RedirectEvent(shortCode, timestamp, null, null, null, null);
  }

  public String shortCode() {
    return shortCode;
  }

  public Instant timestamp() {
    return timestamp;
  }

  public String userAgent() {
    return userAgent;
  }

  public String referer() {
    return referer;
  }

  public String ipHash() {
    return ipHash;
  }

  public String acceptLanguage() {
    return acceptLanguage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RedirectEvent that = (RedirectEvent) o;
    return Objects.equals(shortCode, that.shortCode)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(userAgent, that.userAgent)
        && Objects.equals(referer, that.referer)
        && Objects.equals(ipHash, that.ipHash)
        && Objects.equals(acceptLanguage, that.acceptLanguage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortCode, timestamp, userAgent, referer, ipHash, acceptLanguage);
  }

  @Override
  public String toString() {
    return "RedirectEvent[" +
        "shortCode=" + shortCode +
        ", timestamp=" + timestamp +
        ", userAgent=" + userAgent +
        ", referer=" + referer +
        ", ipHash=" + ipHash +
        ", acceptLanguage=" + acceptLanguage +
        ']';
  }
}