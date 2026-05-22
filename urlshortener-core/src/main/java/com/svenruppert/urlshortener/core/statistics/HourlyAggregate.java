package com.svenruppert.urlshortener.core.statistics;





import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

/**
 * Holds hourly redirect counts for a single day.
 * Used within the hot window for fine-grained intraday analysis.
 * Contains an array of 24 counters, one for each hour (0-23).
 */
public final class HourlyAggregate implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final int HOURS_PER_DAY = 24;

  private final String date;
  private final long[] hourlyCounts;

  public HourlyAggregate(LocalDate date) {
    this.date = Objects.requireNonNull(date, "date must not be null").toString();
    this.hourlyCounts = new long[HOURS_PER_DAY];
  }

  /**
   * Increments the counter for the specified hour.
   *
   * @param hour the hour of day (0-23)
   * @throws IllegalArgumentException if hour is not in range 0-23
   */
  public void increment(int hour) {
    validateHour(hour);
    hourlyCounts[hour]++;
  }

  /**
   * Adds a value to the counter for the specified hour.
   *
   * @param hour  the hour of day (0-23)
   * @param value the value to add
   * @throws IllegalArgumentException if hour is not in range 0-23
   */
  public void add(int hour, long value) {
    validateHour(hour);
    hourlyCounts[hour] += value;
  }

  /**
   * Returns the count for a specific hour.
   *
   * @param hour the hour of day (0-23)
   * @return the redirect count for that hour
   * @throws IllegalArgumentException if hour is not in range 0-23
   */
  public long getCount(int hour) {
    validateHour(hour);
    return hourlyCounts[hour];
  }

  /**
   * Returns the total count across all hours.
   */
  public long totalCount() {
    long total = 0;
    for (long count : hourlyCounts) {
      total += count;
    }
    return total;
  }

  /**
   * Returns a defensive copy of the hourly counts array.
   */
  public long[] hourlyCounts() {
    return Arrays.copyOf(hourlyCounts, HOURS_PER_DAY);
  }

  public LocalDate date() {
    return LocalDate.parse(date);
  }

  /**
   * Finds the hour with the highest redirect count.
   *
   * @return the peak hour (0-23), or 0 if no redirects occurred
   */
  public int peakHour() {
    int peak = 0;
    long maxCount = hourlyCounts[0];
    for (int i = 1; i < HOURS_PER_DAY; i++) {
      if (hourlyCounts[i] > maxCount) {
        maxCount = hourlyCounts[i];
        peak = i;
      }
    }
    return peak;
  }

  private void validateHour(int hour) {
    if (hour < 0 || hour >= HOURS_PER_DAY) {
      throw new IllegalArgumentException("Hour must be between 0 and 23, was: " + hour);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HourlyAggregate that = (HourlyAggregate) o;
    return Objects.equals(date, that.date) && Arrays.equals(hourlyCounts, that.hourlyCounts);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(date);
    result = 31 * result + Arrays.hashCode(hourlyCounts);
    return result;
  }

  @Override
  public String toString() {
    return "HourlyAggregate[" +
        "date=" + date +
        ", totalCount=" + totalCount() +
        ", peakHour=" + peakHour() +
        ']';
  }
}