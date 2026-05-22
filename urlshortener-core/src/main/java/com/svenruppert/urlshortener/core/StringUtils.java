package com.svenruppert.urlshortener.core;





import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class StringUtils {

  private StringUtils() {
  }

  public static boolean isNullOrBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static byte[] charArrayToBytes(char[] chars)
      throws Exception {
    CharsetEncoder encoder = StandardCharsets.UTF_8
        .newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);

    CharBuffer charBuffer = CharBuffer.wrap(chars);
    ByteBuffer byteBuffer = encoder.encode(charBuffer);

    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);

    return bytes;
  }

    public static boolean isBlank(String s) {
      return s == null || s.isBlank();
    }
}