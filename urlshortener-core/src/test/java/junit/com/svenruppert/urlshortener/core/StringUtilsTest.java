package junit.com.svenruppert.urlshortener.core;





import com.svenruppert.urlshortener.core.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

  /**
   * Test class for the StringUtils class.
   * Verifies the functionality of the isNullOrBlank static method, which checks whether
   * a given string is null or contains only whitespace characters.
   */

  @Test
  void testIsNullOrBlank_NullInput() {
    // Test case: Input is null
    String input = null;
    boolean result = StringUtils.isNullOrBlank(input);
    assertTrue(result, "Expected true for null input");
  }

  @Test
  void testIsNullOrBlank_EmptyString() {
    // Test case: Input is an empty string
    String input = "";
    boolean result = StringUtils.isNullOrBlank(input);
    assertTrue(result, "Expected true for empty string input");
  }

  @Test
  void testIsNullOrBlank_WhitespaceString() {
    // Test case: Input is a string containing only whitespace
    String input = "   ";
    boolean result = StringUtils.isNullOrBlank(input);
    assertTrue(result, "Expected true for whitespace string input");
  }

  @Test
  void testIsNullOrBlank_NonEmptyString() {
    // Test case: Input is a non-empty string
    String input = "hello";
    boolean result = StringUtils.isNullOrBlank(input);
    assertFalse(result, "Expected false for non-empty string input");
  }

  @Test
  void testIsNullOrBlank_StringWithWhitespaceAround() {
    // Test case: Input is a string with whitespace around non-empty content
    String input = "   content   ";
    boolean result = StringUtils.isNullOrBlank(input);
    assertFalse(result, "Expected false for string with whitespace and content");
  }

  @Test
  void isNullOrBlank() {
  }
}