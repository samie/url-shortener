package com.svenruppert.urlshortener.core;

/*-
 * #%L
 * core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2023 - 2026 Sven Ruppert
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.urlmapping.ShortenRequest;
import tools.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.svenruppert.dependencies.core.logger.HasLogger.staticLogger;
import static java.util.stream.Collectors.joining;

/**
 * JSON utility facade.
 * <p>
 * Migration note:
 * - This class keeps the public API stable, but delegates JSON work to Jackson (see {@link JacksonJson}).
 * - The old "flat object" parser and manual escaping were removed to avoid subtle parsing bugs.
 */
public final class JsonUtils
    implements HasLogger {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP_REF =
      new TypeReference<>() { };

  private JsonUtils() {
  }

  /* ======================
           READ
     ====================== */

  public static Map<String, String> parseJson(InputStream in)
      throws IOException {
    return toStringMap(JacksonJson.mapper().readValue(in, MAP_REF));
  }


  public static Map<String, String> parseJson(String json)
      throws IOException {
    if (json == null) throw new IOException("JSON is null");
    if (json.isBlank()) return Collections.emptyMap();
    return toStringMap(JacksonJson.mapper().readValue(json, MAP_REF));
  }


  public static Stream<Map.Entry<String, String>> parseJsonToStream(String json)
      throws IOException {
    return parseJson(json).entrySet().stream();
  }

  public static <T> T fromJson(String json, Class<T> type) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("JSON input is null or blank");
    }
    try {
      T value = JacksonJson.mapper().readValue(json, type);
      if (value instanceof ShortenRequest req) {
        if (req.getUrl() == null || req.getUrl().isBlank()) {
          throw new IllegalArgumentException("Field 'url' missing or empty");
        }
      }
      return value;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
    }
  }

  /* ======================
           WRITE
     ====================== */

  public static String toJson(String key, String value) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(key, value);
    return toJson(m);
  }

  // TODO in ein DTO verpacken?
  public static String toJson(String httpCode, String message, String appCode) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("code", httpCode);
    m.put("appCode", appCode);
    m.put("message", message);
    return toJson(m);
  }

  /**
   * Serializes a Map to JSON.
   * (No longer restricted to "flat"; nested structures work correctly.)
   */
  public static String toJson(Map<String, ?> map) {
      return JacksonJson.mapper().writeValueAsString(map);
  }

  /**
   * Serializes DTOs directly via Jackson.
   * Records, Optional, Instant etc. are supported by {@link JacksonJson} configuration.
   */
  public static String toJson(Object dto) {
      return JacksonJson.mapper().writeValueAsString(dto);
  }

  /**
   * Preserved API: builds an array from a list of objects (maps).
   */
  public static String toJsonArrayOfObjects(List<Map<String, String>> items) {
      return JacksonJson.mapper().writeValueAsString(items == null ? List.of() : items);
  }

  public static String toJsonListing(String mode, int count, List<Map<String, String>> items) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("mode", mode);
    root.put("count", count);
    root.put("items", items == null ? List.of() : items);
    return toJson(root);
  }

  public static String toJsonListingPaged(
      String mode,
      int countOnPage,
      List<Map<String, String>> items,
      int page, int size, int total,
      Object sort, Object dir
  ) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("mode", mode);
    root.put("page", page);
    root.put("size", size);
    root.put("total", total);
    if (sort != null) root.put("sort", String.valueOf(sort));
    if (dir != null) root.put("dir", String.valueOf(dir));
    root.put("count", countOnPage);
    root.put("items", items == null ? List.of() : items);
    return toJson(root);
  }

  /* ======================
        SMALL HELPERS
     ====================== */

  public static String extractShortCode(String json)
      throws IOException {
    staticLogger().info("Extracting shortCode from JSON: {}", json);
    Map<String, Object> m;
    m = JacksonJson.mapper().readValue(json, MAP_REF);
    Object sc = m.get("shortCode");
    if (sc == null) throw new IOException("Invalid JSON response - no shortCode available : " + json);

    String shortCode = String.valueOf(sc);
    staticLogger().info("Extracted shortCode: ->|{}|<-", shortCode);
    return shortCode;
  }


  public static String escape(String s) {
    if (s == null) return null;

    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          // JSON: control chars and non-ASCII must be escaped
          if (c < 0x20 || c > 0x7E) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }


  /**
   * Kept for compatibility with existing call sites (if any).
   * Jackson handles escaping correctly; this method now returns the input unchanged.
   */
  public static String unescape(String s) {
    if (s == null) return null;

    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        char n = s.charAt(++i);
        switch (n) {
          case '"' -> out.append('"');
          case '\\' -> out.append('\\');
          case '/' -> out.append('/');
          case 'b' -> out.append('\b');
          case 'f' -> out.append('\f');
          case 'n' -> out.append('\n');
          case 'r' -> out.append('\r');
          case 't' -> out.append('\t');
          case 'u' -> {
            if (i + 4 < s.length()) {
              String hex = s.substring(i + 1, i + 5);
              out.append((char) Integer.parseInt(hex, 16));
              i += 4;
            }
          }
          default -> out.append(n);
        }
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }


  private static String readInputStream(InputStream input)
      throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      return reader.lines().collect(joining());
    }
  }

  private static Map<String, String> toStringMap(Map<String, Object> raw) {
    if (raw == null || raw.isEmpty()) return Collections.emptyMap();
    Map<String, String> out = new LinkedHashMap<>();
    raw.forEach((k, v) -> out.put(k, valueToString(v)));
    return out;
  }

  private static String valueToString(Object v) {
    if (v == null) return null;
    if (v instanceof String s) return s;
    if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);

    // If the server sends nested objects (e.g., maps), keep them as JSON string so existing
    // code that expects "columnInfos" or "changes" as JSON text can still function
    // until those call sites are migrated to proper typed DTOs.
    return JacksonJson.mapper().writeValueAsString(v);
  }
}