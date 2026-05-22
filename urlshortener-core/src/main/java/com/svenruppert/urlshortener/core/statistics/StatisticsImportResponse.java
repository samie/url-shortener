package com.svenruppert.urlshortener.core.statistics;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Response DTO returned by the statistics import endpoint.
 */
public final class StatisticsImportResponse {
  private String mode;
  private long importedEvents;
  private long skippedLines;
  private LocalDate from;
  private LocalDate to;

  public StatisticsImportResponse() {
  }

  public StatisticsImportResponse(String mode,
                                  long importedEvents,
                                  long skippedLines,
                                  LocalDate from,
                                  LocalDate to) {
    this.mode = mode;
    this.importedEvents = importedEvents;
    this.skippedLines = skippedLines;
    this.from = from;
    this.to = to;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public long getImportedEvents() {
    return importedEvents;
  }

  public void setImportedEvents(long importedEvents) {
    this.importedEvents = importedEvents;
  }

  public long getSkippedLines() {
    return skippedLines;
  }

  public void setSkippedLines(long skippedLines) {
    this.skippedLines = skippedLines;
  }

  public LocalDate getFrom() {
    return from;
  }

  public void setFrom(LocalDate from) {
    this.from = from;
  }

  public LocalDate getTo() {
    return to;
  }

  public void setTo(LocalDate to) {
    this.to = to;
  }

  public String mode() {
    return mode;
  }

  public long importedEvents() {
    return importedEvents;
  }

  public long skippedLines() {
    return skippedLines;
  }

  public LocalDate from() {
    return from;
  }

  public LocalDate to() {
    return to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StatisticsImportResponse that = (StatisticsImportResponse) o;
    return importedEvents == that.importedEvents
        && skippedLines == that.skippedLines
        && Objects.equals(mode, that.mode)
        && Objects.equals(from, that.from)
        && Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode, importedEvents, skippedLines, from, to);
  }

  @Override
  public String toString() {
    return "StatisticsImportResponse["
        + "mode=" + mode
        + ", importedEvents=" + importedEvents
        + ", skippedLines=" + skippedLines
        + ", from=" + from
        + ", to=" + to
        + "]";
  }
}
