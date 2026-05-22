package junit.com.svenruppert.urlshortener.core;





import com.svenruppert.urlshortener.core.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

  @Test
  void encodeZeroShouldReturnFirstAlphabetChar() {
    String result = Base62Encoder.encode(0);
    assertEquals("0", result);
  }

  @Test
  void encodeAndDecodeRoundtripForSmallValues() {
    for (long i = 0; i < 1000; i++) {
      String encoded = Base62Encoder.encode(i);
      long decoded = Base62Encoder.decode(encoded);
      assertEquals(i, decoded, "Mismatch at value " + i);
    }
  }

  @Test
  void encodeAndDecodeLargeValue() {
    long original = Long.MAX_VALUE;
    String encoded = Base62Encoder.encode(original);
    long decoded = Base62Encoder.decode(encoded);
    assertEquals(original, decoded);
  }

  @Test
  void decodeShouldRejectNull() {
    assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(null));
  }

  @Test
  void decodeShouldRejectEmptyString() {
    assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
  }

  @Test
  void decodeShouldRejectInvalidCharacters() {
    assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("abc$"));
  }

  @Test
  void encodeShouldRejectNegativeNumbers() {
    assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
  }

  @Test
  void encodeProducesOnlyAlphabetChars() {
    for (long i = 0; i < 10000; i++) {
      String encoded = Base62Encoder.encode(i);
      assertTrue(encoded.matches("[0-9a-zA-Z]+"), "Unexpected characters in: " + encoded);
    }
  }
}