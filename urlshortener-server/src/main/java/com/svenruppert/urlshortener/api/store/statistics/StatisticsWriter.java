package com.svenruppert.urlshortener.api.store.statistics;

import com.svenruppert.urlshortener.core.statistics.RedirectEvent;

import java.time.LocalDate;

/**
 * Interface for recording redirect events.
 * Implementations should be optimized for high-throughput writes.
 */
public interface StatisticsWriter {

  /**
   * Records a redirect event.
   * This method should be non-blocking and return quickly.
   *
   * @param event the redirect event to record
   */
  void recordEvent(RedirectEvent event);

  /**
   * Flushes any buffered events to persistent storage.
   * Called during shutdown or when immediate persistence is required.
   */
  void flush();

  /**
   * Deletes all raw redirect events for the given short code within the
   * inclusive date range. Aggregates are not touched here — call
   * {@link StatisticsStore#reaggregate(LocalDate, LocalDate)} afterwards
   * to keep aggregates consistent.
   *
   * @param shortCode the short code; if null, applies to all known short codes
   * @param from      inclusive start date
   * @param to        inclusive end date
   * @return number of events deleted
   */
  default long deleteEventsInRange(String shortCode, LocalDate from, LocalDate to) {
    return 0;
  }
}
