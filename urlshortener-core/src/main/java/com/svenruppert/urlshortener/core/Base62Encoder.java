package com.svenruppert.urlshortener.core;





public final class Base62Encoder {

  private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int BASE = ALPHABET.length();

  private Base62Encoder() {
  }

  public static String encode(long number) {
    if (number < 0) {
      throw new IllegalArgumentException("Only non-negative values supported");
    }

    StringBuilder result = new StringBuilder();
    do {
      int remainder = (int) (number % BASE);
      result.insert(0, ALPHABET.charAt(remainder));
      number = number / BASE;
    } while (number > 0);

    return result.toString();
  }

  public static long decode(String input) {
    if (input == null || input.isEmpty()) {
      throw new IllegalArgumentException("Input must not be null or empty");
    }

    long result = 0;
    for (char c : input.toCharArray()) {
      int index = ALPHABET.indexOf(c);
      if (index == -1) {
        throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
      }
      result = result * BASE + index;
    }

    return result;
  }
}