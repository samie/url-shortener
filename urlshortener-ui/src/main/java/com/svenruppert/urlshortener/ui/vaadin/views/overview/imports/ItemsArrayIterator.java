package com.svenruppert.urlshortener.ui.vaadin.views.overview.imports;



import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ItemsArrayIterator implements Iterable<String> {

  private final String json;
  private final String field;

  public ItemsArrayIterator(Reader reader, String field) throws IOException {
    this.json = readAll(reader);
    this.field = field;
  }

  @Override
  public Iterator<String> iterator() {
    int arrayStart = findArrayStart(json, field);
    if (arrayStart < 0) {
      // Feld fehlt -> leere Iteration (kein Fehler)
      return Collections.emptyIterator();
    }
    return new ArrayObjectIterator(json, arrayStart);
  }

  private static int findArrayStart(String json, String field) {
    String needle = "\"" + field + "\"";
    int idx = json.indexOf(needle);
    if (idx < 0) return -1;

    idx += needle.length();

    // bis zum ':'
    while (idx < json.length() && json.charAt(idx) != ':') idx++;
    if (idx >= json.length()) return -1;

    idx++; // nach ':'

    // Whitespace skip
    while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
    if (idx >= json.length()) return -1;

    // Array muss mit '[' beginnen
    if (json.charAt(idx) != '[') return -1;

    return idx; // Position des '['
  }

  private static String readAll(Reader r) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[4096];
    int n;
    while ((n = r.read(buf)) >= 0) {
      sb.append(buf, 0, n);
    }
    return sb.toString();
  }

  private static final class ArrayObjectIterator implements Iterator<String> {
    private final String s;
    private int i;
    private String next;

    ArrayObjectIterator(String s, int arrayStartIdx) {
      this.s = s;
      this.i = arrayStartIdx + 1; // direkt nach '['
      advance();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public String next() {
      if (next == null) throw new NoSuchElementException();
      String out = next;
      advance();
      return out;
    }

    private void advance() {
      next = null;

      // whitespace/commas skip
      while (i < s.length()) {
        char c = s.charAt(i);
        if (Character.isWhitespace(c) || c == ',') i++;
        else break;
      }
      if (i >= s.length()) return;

      char c = s.charAt(i);
      if (c == ']') { // Ende Array
        i++;
        return;
      }
      if (c != '{') {
        // Unerwartet -> abbrechen
        return;
      }

      int objStart = i;
      i++; // consume '{'

      boolean inString = false;
      boolean esc = false;
      int depth = 1;

      while (i < s.length() && depth > 0) {
        char ch = s.charAt(i++);
        if (inString) {
          if (esc) esc = false;
          else if (ch == '\\') esc = true;
          else if (ch == '"') inString = false;
        } else {
          if (ch == '"') inString = true;
          else if (ch == '{') depth++;
          else if (ch == '}') depth--;
        }
      }

      if (depth == 0) {
        next = s.substring(objStart, i);
      }
    }
  }
}