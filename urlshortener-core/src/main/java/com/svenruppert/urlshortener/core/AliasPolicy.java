package com.svenruppert.urlshortener.core;





import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AliasPolicy
    implements HasLogger {

  public static final int MIN = 3;
  public static final int MAX = 32;
  public static final String REGEX_ALLOWED = "^[A-Za-z0-9_-]+$";
  private static final Pattern ALLOWED = Pattern.compile(REGEX_ALLOWED);
  //TODO - must be dynamically validated against all paths of the UI.
  private static final Set<String> RESERVED = Set.of("api", "about", "create", "delete", "list");

  private AliasPolicy() {
  }

  public static Validation validate(String alias) {
    HasLogger.staticLogger().info("validate - {}", alias);
    if (alias == null || alias.isBlank()) return Validation.fail(Reason.NULL_OR_BLANK);
    if (alias.length() < MIN) return Validation.fail(Reason.TOO_SHORT);
    if (alias.length() > MAX) return Validation.fail(Reason.TOO_LONG);
    if (!ALLOWED.matcher(alias).matches()) return Validation.fail(Reason.INVALID_CHARS);
    if (RESERVED.contains(alias.toLowerCase())) return Validation.fail(Reason.RESERVED);
    return Validation.ok();
  }

  public static String normalize(String alias) {
    return alias.toLowerCase(Locale.ROOT);
  }

  //TODO - i18n
  public enum Reason {
    NULL_OR_BLANK("Alias must not be empty."),
    TOO_SHORT("Alias is too short (min " + MIN + ")."),
    TOO_LONG("Alias is too long (max " + MAX + ")."),
    INVALID_CHARS("Alias contains invalid characters (allowed: [A-Za-z0-9_-])."),
    RESERVED("Alias is reserved and cannot be used.");

    public final String defaultMessage;

    Reason(String msg) {
      this.defaultMessage = msg;
    }
  }

  public record Validation(boolean valid, Reason reason) {
    public static Validation ok() {
      return new Validation(true, null);
    }

    public static Validation fail(Reason r) {
      return new Validation(false, r);
    }

    public boolean failed() {
      return !valid;
    }
  }
}