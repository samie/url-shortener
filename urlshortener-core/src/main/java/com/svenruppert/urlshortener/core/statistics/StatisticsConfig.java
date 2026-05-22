package com.svenruppert.urlshortener.core.statistics;





import java.io.Serial;
import java.io.Serializable;

/**
 * Configuration for the statistics system.
 * Stored in EclipseStore to persist across restarts and allow UI editing.
 */
public final class StatisticsConfig implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Default hot window size in days.
   */
  public static final int DEFAULT_HOT_WINDOW_DAYS = 7;

  /**
   * Default batch size for the event writer thread.
   */
  public static final int DEFAULT_WRITER_BATCH_SIZE = 100;

  /**
   * Default interval in seconds for the aggregator thread.
   */
  public static final int DEFAULT_AGGREGATOR_INTERVAL_SECONDS = 3600;

  private int hotWindowDays;
  private int writerBatchSize;
  private int aggregatorIntervalSeconds;
  private boolean statisticsEnabled;

  /**
   * Creates a configuration with default values.
   */
  public StatisticsConfig() {
    this.hotWindowDays = DEFAULT_HOT_WINDOW_DAYS;
    this.writerBatchSize = DEFAULT_WRITER_BATCH_SIZE;
    this.aggregatorIntervalSeconds = DEFAULT_AGGREGATOR_INTERVAL_SECONDS;
    this.statisticsEnabled = true;
  }

  /**
   * Returns the number of days to keep in the hot window.
   * Within this window, hourly aggregates are maintained and ad-hoc queries
   * are computed from raw events.
   */
  public int hotWindowDays() {
    return hotWindowDays;
  }

  /**
   * Sets the hot window size in days.
   *
   * @param days number of days (must be at least 1)
   * @throws IllegalArgumentException if days is less than 1
   */
  public void setHotWindowDays(int days) {
    if (days < 1) {
      throw new IllegalArgumentException("Hot window must be at least 1 day, was: " + days);
    }
    this.hotWindowDays = days;
  }

  /**
   * Returns the batch size for the event writer thread.
   * Events are collected up to this size before being persisted.
   */
  public int writerBatchSize() {
    return writerBatchSize;
  }

  /**
   * Sets the batch size for the event writer.
   *
   * @param size batch size (must be at least 1)
   * @throws IllegalArgumentException if size is less than 1
   */
  public void setWriterBatchSize(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Writer batch size must be at least 1, was: " + size);
    }
    this.writerBatchSize = size;
  }

  /**
   * Returns the interval in seconds between aggregator runs.
   */
  public int aggregatorIntervalSeconds() {
    return aggregatorIntervalSeconds;
  }

  /**
   * Sets the aggregator interval in seconds.
   *
   * @param seconds interval in seconds (must be at least 60)
   * @throws IllegalArgumentException if seconds is less than 60
   */
  public void setAggregatorIntervalSeconds(int seconds) {
    if (seconds < 60) {
      throw new IllegalArgumentException("Aggregator interval must be at least 60 seconds, was: " + seconds);
    }
    this.aggregatorIntervalSeconds = seconds;
  }

  /**
   * Returns whether statistics collection is enabled.
   */
  public boolean isStatisticsEnabled() {
    return statisticsEnabled;
  }

  /**
   * Enables or disables statistics collection.
   */
  public void setStatisticsEnabled(boolean enabled) {
    this.statisticsEnabled = enabled;
  }

  @Override
  public String toString() {
    return "StatisticsConfig[" +
        "hotWindowDays=" + hotWindowDays +
        ", writerBatchSize=" + writerBatchSize +
        ", aggregatorIntervalSeconds=" + aggregatorIntervalSeconds +
        ", statisticsEnabled=" + statisticsEnabled +
        ']';
  }
}