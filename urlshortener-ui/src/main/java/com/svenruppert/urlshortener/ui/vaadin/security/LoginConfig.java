package com.svenruppert.urlshortener.ui.vaadin.security;




import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Central configuration for the simple admin login.
 * Reads its values from auth.properties via LoginConfigInitializer.
 */
public final class LoginConfig
    implements HasLogger {

  private static volatile boolean loginEnabled;
  private static volatile byte[] expectedPasswordBytes;

  private LoginConfig() {
  }

  /**
   * Initialises the login configuration.
   *
   * @param enabled  whether the login mechanism should be enforced
   * @param password the raw password read from configuration, may be {@code null}
   */
  public static void initialise(boolean enabled, String password) {
    loginEnabled = enabled;

    if (!enabled || password == null || password.isBlank()) {
      expectedPasswordBytes = null;
      return;
    }

    expectedPasswordBytes = password.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @return {@code true} if login protection is enabled at all
   */
  public static boolean isLoginEnabled() {
    return loginEnabled;
  }

  /**
   * @return {@code true} if login is enabled and a usable password has been configured
   */
  public static boolean isLoginConfigured() {
    return loginEnabled
        && expectedPasswordBytes != null
        && expectedPasswordBytes.length > 0;
  }

  /**
   * Compares the entered password with the configured one using constant-time comparison.
   */
  public static boolean matches(char[] enteredPassword) {
    if (!isLoginConfigured() || enteredPassword == null) {
      return false;
    }
    try {
      byte[] entered = StringUtils.charArrayToBytes(enteredPassword);
      boolean result = MessageDigest.isEqual(expectedPasswordBytes, entered);
      Arrays.fill(entered, (byte) 0);
      return result;
    } catch (Exception e) {
      HasLogger.staticLogger().warn("matches - {}", e.getMessage());
      return false;
    }

  }
}