package com.svenruppert.urlshortener.api.handler.statistics.exports;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsReader;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_EXPORT_FORMAT_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class StatisticsExportWriter
    implements HasLogger {

  private static final byte[] NEWLINE = {'\n'};

  private final StatisticsReader reader;

  public StatisticsExportWriter(StatisticsReader reader) {
    this.reader = reader;
  }

  public Result writeNdjsonTo(OutputStream out,
                              Instant exportedAt,
                              LocalDate from,
                              LocalDate to,
                              Collection<String> shortCodes)
      throws IOException {

    List<String> codes = (shortCodes == null || shortCodes.isEmpty())
        ? new ArrayList<>(reader.getKnownShortCodes())
        : new ArrayList<>(shortCodes);

    LocalDate effectiveFrom = (from != null) ? from : earliestDate(codes);
    LocalDate effectiveTo = (to != null) ? to : LocalDate.now(ZoneOffset.UTC);

    writeHeader(out, exportedAt, effectiveFrom, effectiveTo, codes);

    long eventCount = 0;
    for (String code : codes) {
      LocalDate current = effectiveFrom;
      while (current != null && !current.isAfter(effectiveTo)) {
        var events = reader.getEventsForDate(code, current);
        for (RedirectEvent event : events) {
          writeEvent(out, event);
          eventCount++;
        }
        current = current.plusDays(1);
      }
    }

    return new Result(codes.size(), eventCount, effectiveFrom, effectiveTo);
  }

  private LocalDate earliestDate(List<String> codes) {
    LocalDate earliest = null;
    for (String code : codes) {
      for (LocalDate d : reader.getAvailableDates(code)) {
        if (earliest == null || d.isBefore(earliest)) {
          earliest = d;
        }
      }
    }
    return (earliest != null) ? earliest : LocalDate.now(ZoneOffset.UTC);
  }

  private void writeHeader(OutputStream out,
                           Instant exportedAt,
                           LocalDate from,
                           LocalDate to,
                           List<String> codes)
      throws IOException {
    Map<String, Object> header = new LinkedHashMap<>();
    header.put("type", "header");
    header.put("formatVersion", STATISTICS_EXPORT_FORMAT_VERSION);
    header.put("exportedAt", exportedAt.toString());
    Map<String, Object> range = new LinkedHashMap<>();
    range.put("from", from.toString());
    range.put("to", to.toString());
    header.put("range", range);
    header.put("shortCodes", codes);
    out.write(JsonUtils.toJson(header).getBytes(UTF_8));
    out.write(NEWLINE);
  }

  private void writeEvent(OutputStream out, RedirectEvent event)
      throws IOException {
    Map<String, Object> obj = new LinkedHashMap<>();
    obj.put("shortCode", event.shortCode());
    obj.put("timestamp", event.timestamp().toString());
    if (event.userAgent() != null) obj.put("userAgent", event.userAgent());
    if (event.referer() != null) obj.put("referer", event.referer());
    if (event.ipHash() != null) obj.put("ipHash", event.ipHash());
    if (event.acceptLanguage() != null) obj.put("acceptLanguage", event.acceptLanguage());
    out.write(JsonUtils.toJson(obj).getBytes(UTF_8));
    out.write(NEWLINE);
  }

  public record Result(int shortCodeCount, long eventCount, LocalDate from, LocalDate to) { }
}
