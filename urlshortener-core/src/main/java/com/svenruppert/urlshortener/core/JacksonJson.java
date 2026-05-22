package com.svenruppert.urlshortener.core;







import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Central Jackson configuration for the whole application.
 * Keep this as the single source of truth for JSON behaviour.
 */
public final class JacksonJson {

  private static final ObjectMapper MAPPER = initMapper();

  private JacksonJson() {
    // utility class
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /**
   * Convenience: serialize any value to JSON string.
   */
  public static String toJson(Object value) {
      return MAPPER.writeValueAsString(value);
  }

  /**
   * Convenience: deserialize JSON string into a target type.
   */
  public static <T> T fromJson(String json, Class<T> type) {
      return MAPPER.readValue(json, type);
  }

  /**
   * Convenience: read JSON from stream.
   */
  public static <T> T fromJson(InputStream in, Class<T> type)
      throws IOException {
    return MAPPER.readValue(in, type);
  }

  /**
   * Convenience: write JSON to stream.
   */
  public static void toJson(OutputStream out, Object value)
      throws IOException {
    MAPPER.writeValue(out, value);
  }

  /**
   * Create a streaming JsonGenerator for large exports.
   * Caller is responsible for closing it.
   */
  public static JsonGenerator newGenerator(OutputStream out)
      throws IOException {
    return MAPPER.createGenerator(out);
  }

  /**
   * Create a streaming JsonParser for large imports.
   * Caller is responsible for closing it.
   */
  public static JsonParser newParser(InputStream in)
      throws IOException {
    return MAPPER.createParser(in);
  }

  private static ObjectMapper initMapper() {
    try {
      return createMapperWithOptionalConstraints();
    } catch (Throwable ignored) {
      // Never fail class initialization because of optional JSON tuning.
      // This keeps API handlers functional even with slightly different runtime classpaths.
      return new ObjectMapper();
    }
  }

  private static ObjectMapper createMapperWithOptionalConstraints() {
    JsonFactory factory = createFactoryWithOptionalConstraints();
    ObjectMapper mapper = (factory == null) ? new ObjectMapper() : new ObjectMapper(factory);
    // Jackson3: mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // >>> Wichtig für Klassen mit "record-style" Accessors: shortCode(), originalUrl(), ...
    //    mapper.(
    //        new DefaultAccessorNamingStrategy
    //            .Provider()
    //            .withGetterPrefix("")      // akzeptiert foo() als Getter
    //            .withSetterPrefix("with")); // optional; siehe Hinweis unten

    // Records support: Jackson 2.12+ supports records out of the box when using databind.
    //    mapper.registerModule(new JavaTimeModule());
    //    mapper.registerModule(new Jdk8Module());

    // Instant, LocalDateTime etc. as ISO-8601, not timestamps
    //    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Robustness / forward compatibility
    //    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Keep payloads compact; adjust if you need nulls explicitly
    //    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // Often useful for debugging; disable if you want compact JSON everywhere
    // mapper.enable(SerializationFeature.INDENT_OUTPUT);

    return mapper;
  }

  private static JsonFactory createFactoryWithOptionalConstraints() {
    try {
      Method factoryBuilderMethod = JsonFactory.class.getMethod("builder");
      Object factoryBuilder = factoryBuilderMethod.invoke(null);

      Class<?> readConstraintsClass = Class.forName("com.fasterxml.jackson.core.StreamReadConstraints");
      Class<?> writeConstraintsClass = Class.forName("com.fasterxml.jackson.core.StreamWriteConstraints");

      Method readBuilderMethod = readConstraintsClass.getMethod("builder");
      Object readBuilder = readBuilderMethod.invoke(null);
      readBuilder.getClass().getMethod("maxStringLength", int.class).invoke(readBuilder, 10_000_000);
      readBuilder.getClass().getMethod("maxNumberLength", int.class).invoke(readBuilder, 1_000);
      Object readConstraints = readBuilder.getClass().getMethod("build").invoke(readBuilder);

      Method writeBuilderMethod = writeConstraintsClass.getMethod("builder");
      Object writeBuilder = writeBuilderMethod.invoke(null);
      writeBuilder.getClass().getMethod("maxNestingDepth", int.class).invoke(writeBuilder, 1_000);
      Object writeConstraints = writeBuilder.getClass().getMethod("build").invoke(writeBuilder);

      factoryBuilder.getClass()
          .getMethod("streamReadConstraints", readConstraintsClass)
          .invoke(factoryBuilder, readConstraints);
      factoryBuilder.getClass()
          .getMethod("streamWriteConstraints", writeConstraintsClass)
          .invoke(factoryBuilder, writeConstraints);

      return (JsonFactory) factoryBuilder.getClass().getMethod("build").invoke(factoryBuilder);
    } catch (Throwable ignored) {
      return null;
    }
  }
}