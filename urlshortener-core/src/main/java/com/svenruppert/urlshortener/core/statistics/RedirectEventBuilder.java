package com.svenruppert.urlshortener.core.statistics;





import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Builder for creating RedirectEvent instances.
 * Provides convenience methods for extracting data from HTTP requests
 * and handles IP hashing for privacy compliance.
 */
public final class RedirectEventBuilder {

  private static final String HASH_ALGORITHM = "SHA-256";
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  private final String shortCode;
  private final Clock clock;

  private Instant timestamp;
  private String userAgent;
  private String referer;
  private String ipHash;
  private String acceptLanguage;

  private RedirectEventBuilder(String shortCode, Clock clock) {
    this.shortCode = Objects.requireNonNull(shortCode, "shortCode must not be null");
    this.clock = clock != null ? clock : Clock.systemUTC();
  }

  /**
   * Creates a new builder for the given short code.
   */
  public static RedirectEventBuilder forShortCode(String shortCode) {
    return new RedirectEventBuilder(shortCode, null);
  }

  /**
   * Creates a new builder with a custom clock (useful for testing).
   */
  public static RedirectEventBuilder forShortCode(String shortCode, Clock clock) {
    return new RedirectEventBuilder(shortCode, clock);
  }

  /**
   * Sets the timestamp. If not called, the current time will be used.
   */
  public RedirectEventBuilder timestamp(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Sets the User-Agent header value.
   */
  public RedirectEventBuilder userAgent(String userAgent) {
    this.userAgent = sanitize(userAgent);
    return this;
  }

  /**
   * Sets the Referer header value.
   */
  public RedirectEventBuilder referer(String referer) {
    this.referer = sanitize(referer);
    return this;
  }

  /**
   * Sets the IP address and hashes it for privacy.
   * The original IP is never stored.
   */
  public RedirectEventBuilder ipAddress(String ipAddress) {
    this.ipHash = hashIp(ipAddress);
    return this;
  }

  /**
   * Sets an already hashed IP value directly.
   */
  public RedirectEventBuilder ipHash(String ipHash) {
    this.ipHash = ipHash;
    return this;
  }

  /**
   * Sets the Accept-Language header value.
   */
  public RedirectEventBuilder acceptLanguage(String acceptLanguage) {
    this.acceptLanguage = sanitize(acceptLanguage);
    return this;
  }

  /**
   * Builds the RedirectEvent.
   */
  public RedirectEvent build() {
    Instant ts = timestamp != null ? timestamp : clock.instant();
    return new RedirectEvent(
        shortCode,
        ts,
        userAgent,
        referer,
        ipHash,
        acceptLanguage
    );
  }

  /**
   * Hashes an IP address using SHA-256.
   * Returns null if the input is null or empty.
   */
  private String hashIp(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
      // Return first 16 characters of the hex string for a shorter hash
      return HEX_FORMAT.formatHex(hash).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 should always be available
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Sanitizes header values by trimming and limiting length.
   */
  private String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    // Limit to 500 characters to prevent storage bloat
    if (trimmed.length() > 500) {
      return trimmed.substring(0, 500);
    }
    return trimmed;
  }
}