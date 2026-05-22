package com.svenruppert.urlshortener.api.store.statistics;

import com.svenruppert.urlshortener.core.statistics.StatisticsConfig;

import java.time.LocalDate;
import java.util.Map;

/**
 * Combined interface for statistics storage operations.
 * Extends both reader and writer interfaces and adds administrative methods.
 */
public interface StatisticsStore extends StatisticsReader, StatisticsWriter {

  /**
   * Returns the current statistics configuration.
   */
  StatisticsConfig getConfig();

  /**
   * Updates the statistics configuration.
   * Changes are persisted immediately.
   *
   * @param config the new configuration
   */
  void updateConfig(StatisticsConfig config);

  /**
   * Removes all statistics data for a given short code.
   * Called when a URL mapping is deleted.
   *
   * @param shortCode the short code to remove
   */
  void removeAllForShortCode(String shortCode);

  /**
   * Starts background processing threads (writer, aggregator).
   * Should be called once during application startup.
   */
  void start();

  /**
   * Stops background processing threads gracefully.
   * Should be called during application shutdown.
   */
  void stop();

  /**
   * Returns debug information about the internal state of the statistics store.
   * Used for debugging persistence issues.
   */
  default Map<String, Object> getDebugInfo() {
    return Map.of("message", "Debug info not implemented for this store type");
  }

  /**
   * Rebuilds hourly and daily aggregates from raw redirect events for the
   * inclusive date range. Existing aggregates within that range are
   * discarded first to avoid double counting; raw events stay untouched.
   *
   * @param from inclusive start date
   * @param to   inclusive end date
   * @return number of (shortCode, date) buckets rebuilt
   */
  default long reaggregate(LocalDate from, LocalDate to) {
    return 0;
  }
}
