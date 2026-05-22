package com.svenruppert.urlshortener.core.statistics;





import java.time.LocalDate;
import java.util.Objects;

/**
 * Response DTO for statistics count queries.
 */
public final class StatisticsCountResponse {
  private String shortCode;
  private LocalDate from;
  private LocalDate to;
  private long count;

  public StatisticsCountResponse() {
  }

  /**
   *
   */
  public StatisticsCountResponse(
      String shortCode,
      LocalDate from,
      LocalDate to,
      long count
  ) {
    this.shortCode = shortCode;
    this.from = from;
    this.to = to;
    this.count = count;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
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

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  /**
   * Creates a response for a single date query.
   */
  public static StatisticsCountResponse forDate(String shortCode, LocalDate date, long count) {
    return new StatisticsCountResponse(shortCode, date, date, count);
  }

  /**
   * Creates a response for a total count query (all time).
   */
  public static StatisticsCountResponse total(String shortCode, long count) {
    return new StatisticsCountResponse(shortCode, null, null, count);
  }

  public String shortCode() {
    return shortCode;
  }

  public LocalDate from() {
    return from;
  }

  public LocalDate to() {
    return to;
  }

  public long count() {
    return count;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (StatisticsCountResponse) obj;
    return Objects.equals(this.shortCode, that.shortCode) &&
        Objects.equals(this.from, that.from) &&
        Objects.equals(this.to, that.to) &&
        this.count == that.count;
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortCode, from, to, count);
  }

  @Override
  public String toString() {
    return "StatisticsCountResponse[" +
        "shortCode=" + shortCode + ", " +
        "from=" + from + ", " +
        "to=" + to + ", " +
        "count=" + count + ']';
  }

}