package com.svenruppert.urlshortener.core.statistics;





import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Holds aggregated redirect statistics for a single day.
 * This aggregate is permanent and survives beyond the hot window.
 * Contains the total count and can be extended with additional metrics.
 */
public final class DailyAggregate implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String date;
  private long totalCount;
  private long uniqueVisitors;

  public DailyAggregate(LocalDate date) {
    this.date = Objects.requireNonNull(date, "date must not be null").toString();
    this.totalCount = 0;
    this.uniqueVisitors = 0;
  }

  public DailyAggregate(LocalDate date, long totalCount, long uniqueVisitors) {
    this.date = Objects.requireNonNull(date, "date must not be null").toString();
    this.totalCount = totalCount;
    this.uniqueVisitors = uniqueVisitors;
  }

  /**
   * Creates a DailyAggregate from an HourlyAggregate.
   * Used when an HourlyAggregate is being retired from the hot window.
   */
  public static DailyAggregate fromHourlyAggregate(HourlyAggregate hourly) {
    return new DailyAggregate(hourly.date(), hourly.totalCount(), 0);
  }

  /**
   * Increments the total count by one.
   */
  public void increment() {
    totalCount++;
  }

  /**
   * Adds a value to the total count.
   */
  public void add(long value) {
    totalCount += value;
  }

  /**
   * Sets the unique visitor count.
   * This is typically calculated during aggregation from raw events.
   */
  public void setUniqueVisitors(long uniqueVisitors) {
    this.uniqueVisitors = uniqueVisitors;
  }

  public LocalDate date() {
    return LocalDate.parse(date);
  }

  public long totalCount() {
    return totalCount;
  }

  public long uniqueVisitors() {
    return uniqueVisitors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DailyAggregate that = (DailyAggregate) o;
    return totalCount == that.totalCount
        && uniqueVisitors == that.uniqueVisitors
        && Objects.equals(date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, totalCount, uniqueVisitors);
  }

  @Override
  public String toString() {
    return "DailyAggregate[" +
        "date=" + date +
        ", totalCount=" + totalCount +
        ", uniqueVisitors=" + uniqueVisitors +
        ']';
  }
}