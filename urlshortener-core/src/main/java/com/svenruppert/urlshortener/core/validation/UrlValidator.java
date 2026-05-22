package com.svenruppert.urlshortener.core.validation;





import java.net.URI;
import java.util.Objects;

/**
 * Reusable, framework-independent URL validator.
 * Accepts only http/https URLs with a valid host and no control characters.
 */
public final class UrlValidator {

  private static final int MAX_LENGTH = 2000;

  private UrlValidator() {
  }

  public static ValidationResult validate(String value) {
    if (value == null) {
      return ValidationResult.error("URL must not be null");
    }
    String v = value.trim();
    if (v.isEmpty()) {
      return ValidationResult.error("URL must not be blank");
    }
    if (v.length() > MAX_LENGTH) {
      return ValidationResult.error("URL exceeds maximum length of " + MAX_LENGTH);
    }
    if (v.chars().anyMatch(ch -> Character.isWhitespace(ch) || Character.isISOControl(ch))) {
      return ValidationResult.error("URL must not contain whitespace or control characters");
    }

    int schemeSep = v.indexOf("://");
    if (schemeSep <= 0) {
      return ValidationResult.error("URL must include scheme, e.g. https://example.org");
    }

    String scheme = v.substring(0, schemeSep).toLowerCase();
    if (!scheme.equals("http") && !scheme.equals("https")) {
      return ValidationResult.error("Only http and https URLs are allowed");
    }

    try {
      URI uri = URI.create(v);
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        return ValidationResult.error("Missing or invalid host");
      }
      // 1) Host darf nur a–z, 0–9, -, . enthalten
      if (!host.matches("^[a-zA-Z0-9.-]+$")) {
        return ValidationResult.error("Host contains invalid characters");
      }

      // 2) Host muss mindestens eine '.' enthalten (also echte Domain)
      if (!host.contains(".")) {
        return ValidationResult.error("Host must contain a dot (e.g. example.com)");
      }

      // 3) Kein Punkt am Anfang/Ende
      if (host.startsWith(".") || host.endsWith(".")) {
        return ValidationResult.error("Host must not start or end with a dot");
      }

      // 4) Keine aufeinanderfolgenden Punkte
      if (host.contains("..")) {
        return ValidationResult.error("Host must not contain consecutive dots");
      }

      // 5) Optional: minimale Länge der TLD (z. B. 2–63 Zeichen)
      String[] parts = host.split("\\.");
      String tld = parts[parts.length - 1];
      if (tld.length() < 2 || tld.length() > 63) {
        return ValidationResult.error("Invalid top-level domain");
      }
      return ValidationResult.ok();
    } catch (IllegalArgumentException ex) {
      return ValidationResult.error("Malformed URL");
    }
  }

  /**
   * Lightweight value object for validation results.
   */
  public record ValidationResult(boolean valid, String message) {
    public static ValidationResult ok() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult error(String msg) {
      return new ValidationResult(false, Objects.requireNonNull(msg));
    }
  }
}