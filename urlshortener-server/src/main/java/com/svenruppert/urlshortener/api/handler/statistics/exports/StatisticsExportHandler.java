package com.svenruppert.urlshortener.api.handler.statistics.exports;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsReader;
import com.svenruppert.urlshortener.api.utils.ErrorResponses;
import com.svenruppert.urlshortener.api.utils.RequestMethodUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static com.svenruppert.dependencies.core.net.HttpStatus.OK;
import static com.svenruppert.urlshortener.api.handler.urlmapping.exports.ZipWriter.writeZipStream;
import static com.svenruppert.urlshortener.api.utils.QueryUtils.first;
import static com.svenruppert.urlshortener.api.utils.QueryUtils.parseQueryParams;
import static com.svenruppert.urlshortener.core.DefaultValues.*;

public class StatisticsExportHandler
    implements HttpHandler, HasLogger {

  private static final DateTimeFormatter EXPORT_TS =
      DateTimeFormatter.ofPattern(PATTERN_DATE_TIME_EXPORT)
          .withZone(ZoneOffset.UTC);

  private final StatisticsReader reader;

  public StatisticsExportHandler(StatisticsReader reader) {
    this.reader = reader;
  }

  @Override
  public void handle(HttpExchange ex)
      throws IOException {
    if (!RequestMethodUtils.requireGetOrHead(ex)) return;

    var query = parseQueryParams(Optional.ofNullable(ex.getRequestURI().getRawQuery()).orElse(""));

    LocalDate from;
    LocalDate to;
    try {
      from = parseDate(first(query, "from"));
      to = parseDate(first(query, "to"));
    } catch (DateTimeParseException e) {
      ErrorResponses.badRequest(ex, "invalid_date",
                                "Invalid date format. Use ISO format: yyyy-MM-dd");
      return;
    }
    if (from != null && to != null && from.isAfter(to)) {
      ErrorResponses.badRequest(ex, "invalid_range", "from must not be after to");
      return;
    }

    Set<String> shortCodes = parseShortCodes(first(query, "shortCodes"));

    Instant exportedAt = resolveExportInstant(query);
    String timestamp = EXPORT_TS.format(exportedAt);
    String filename = STATISTICS_EXPORT_FILE_NAME + "-" + timestamp + ".zip";

    if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
      logger().info("HEAD statistics export — filename={}", filename);
      var headers = ex.getResponseHeaders();
      headers.add(CONTENT_TYPE, APPLICATION_ZIP);
      headers.add(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
      headers.add(EXPORT_TIMESTAMP_HEADER, exportedAt.toString());
      ex.sendResponseHeaders(OK.code(), -1);
      ex.close();
      return;
    }

    logger().info("GET statistics export — from={} to={} shortCodes={} filename={}",
                  from, to, shortCodes.isEmpty() ? "<all>" : shortCodes, filename);

    var writer = new StatisticsExportWriter(reader);
    writeZipStream(ex,
                   OK,
                   STATISTICS_EXPORT_NDJSON_ENTRY,
                   filename,
                   out -> writer.writeNdjsonTo(out, exportedAt, from, to, shortCodes));
  }

  private Instant resolveExportInstant(java.util.Map<String, java.util.List<String>> query) {
    String ts = first(query, EXPORT_TIMESTAMP_PARAM);
    if (ts != null && !ts.isBlank()) {
      try {
        return Instant.parse(ts);
      } catch (Exception ignore) {
        // fall through to "now"
      }
    }
    return Instant.now();
  }

  private LocalDate parseDate(String s) {
    if (s == null || s.isBlank()) return null;
    return LocalDate.parse(s.trim());
  }

  private Set<String> parseShortCodes(String raw) {
    if (raw == null || raw.isBlank()) return Set.of();
    Set<String> codes = new LinkedHashSet<>();
    Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .forEach(codes::add);
    return codes;
  }
}
