package com.svenruppert.urlshortener.core.urlmapping.imports;





import java.io.IOException;

public class JsonFieldExtractor {
  private JsonFieldExtractor() {

  }

  public static String extractJsonString(String json, String field)
      throws IOException {
    String needle = "\"" + field + "\":";
    int idx = json.indexOf(needle);
    if (idx < 0) throw new IOException("Field not found: " + field);
    idx += needle.length();

    while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
    if (idx >= json.length() || json.charAt(idx) != '"') throw new IOException("Expected string for: " + field);

    idx++; // after opening quote
    StringBuilder sb = new StringBuilder();
    boolean esc = false;
    while (idx < json.length()) {
      char c = json.charAt(idx++);
      if (esc) {
        sb.append(c);
        esc = false;
        continue;
      }
      if (c == '\\') {
        esc = true;
        continue;
      }
      if (c == '"') return sb.toString();
      sb.append(c);
    }
    throw new IOException("Unterminated string for: " + field);
  }

  public static int extractJsonInt(String json, String field, int def) {
    String needle = "\"" + field + "\":";
    int idx = json.indexOf(needle);
    if (idx < 0) return def;
    idx += needle.length();
    while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
    int start = idx;
    while (idx < json.length() && (json.charAt(idx) == '-' || Character.isDigit(json.charAt(idx)))) idx++;
    try {
      return Integer.parseInt(json.substring(start, idx));
    } catch (Exception e) {
      return def;
    }
  }
}