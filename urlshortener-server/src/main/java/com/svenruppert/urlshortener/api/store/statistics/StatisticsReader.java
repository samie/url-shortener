package com.svenruppert.urlshortener.api.store.statistics;

import com.svenruppert.urlshortener.core.statistics.DailyAggregate;
import com.svenruppert.urlshortener.core.statistics.HourlyAggregate;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for querying redirect statistics.
 * Provides methods for both raw event access and aggregate queries.
 */
public interface StatisticsReader {

  /**
   * Returns the total redirect count for a short code on a specific date.
   *
   * @param shortCode the short code
   * @param date      the date
   * @return the total count, or 0 if no data exists
   */
  long getCountForDate(String shortCode, LocalDate date);

  /**
   * Returns the total redirect count for a short code within a date range (inclusive).
   *
   * @param shortCode the short code
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return the total count
   */
  long getCountForDateRange(String shortCode, LocalDate from, LocalDate to);

  /**
   * Returns the total redirect count for a short code across all time.
   *
   * @param shortCode the short code
   * @return the total count
   */
  long getTotalCount(String shortCode);

  /**
   * Returns the hourly aggregate for a specific date, if available.
   * Only available within the hot window.
   *
   * @param shortCode the short code
   * @param date      the date
   * @return the hourly aggregate, or empty if not in hot window
   */
  Optional<HourlyAggregate> getHourlyAggregate(String shortCode, LocalDate date);

  /**
   * Returns the daily aggregate for a specific date, if available.
   *
   * @param shortCode the short code
   * @param date      the date
   * @return the daily aggregate, or empty if no data exists
   */
  Optional<DailyAggregate> getDailyAggregate(String shortCode, LocalDate date);

  /**
   * Returns all daily aggregates for a short code within a date range.
   *
   * @param shortCode the short code
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return list of daily aggregates, sorted by date ascending
   */
  List<DailyAggregate> getDailyAggregates(String shortCode, LocalDate from, LocalDate to);

  /**
   * Returns all hourly aggregates for a short code within a date range.
   * Only returns data within the hot window.
   *
   * @param shortCode the short code
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return list of hourly aggregates, sorted by date ascending
   */
  List<HourlyAggregate> getHourlyAggregates(String shortCode, LocalDate from, LocalDate to);

  /**
   * Returns raw redirect events for a short code on a specific date.
   *
   * @param shortCode the short code
   * @param date      the date
   * @return list of events, sorted by timestamp ascending
   */
  List<RedirectEvent> getEventsForDate(String shortCode, LocalDate date);

  /**
   * Returns raw redirect events for a short code within a date range.
   *
   * @param shortCode the short code
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return list of events, sorted by timestamp ascending
   */
  List<RedirectEvent> getEventsForDateRange(String shortCode, LocalDate from, LocalDate to);

  /**
   * Returns the dates for which data exists for a short code.
   *
   * @param shortCode the short code
   * @return list of dates with data, sorted ascending
   */
  List<LocalDate> getAvailableDates(String shortCode);

  /**
   * Checks if the given date is within the current hot window.
   *
   * @param date the date to check
   * @return true if the date is within the hot window
   */
  boolean isInHotWindow(LocalDate date);

  /**
   * Returns all known short codes that have any statistics data
   * (raw events or aggregates) recorded.
   *
   * @return set of known short codes (may be empty, never null)
   */
  default Set<String> getKnownShortCodes() {
    return Set.of();
  }
}
