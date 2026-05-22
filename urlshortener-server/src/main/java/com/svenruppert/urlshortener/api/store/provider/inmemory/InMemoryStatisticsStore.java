package com.svenruppert.urlshortener.api.store.provider.inmemory;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsStore;
import com.svenruppert.urlshortener.core.statistics.DailyAggregate;
import com.svenruppert.urlshortener.core.statistics.HourlyAggregate;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import com.svenruppert.urlshortener.core.statistics.StatisticsConfig;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory implementation of StatisticsStore for testing purposes.
 * Events are processed synchronously (no background threads).
 * All data is lost when the JVM shuts down.
 */
public class InMemoryStatisticsStore
    implements StatisticsStore, HasLogger {

  private final Map<String, Map<LocalDate, List<RedirectEvent>>> events = new ConcurrentHashMap<>();
  private final Map<String, Map<LocalDate, HourlyAggregate>> hourlyAggregates = new ConcurrentHashMap<>();
  private final Map<String, Map<LocalDate, DailyAggregate>> dailyAggregates = new ConcurrentHashMap<>();

  private final StatisticsConfig config = new StatisticsConfig();
  private final Clock clock;

  /**
   * Creates a new InMemoryStatisticsStore with the system UTC clock.
   */
  public InMemoryStatisticsStore() {
    this(Clock.systemUTC());
  }

  /**
   * Creates a new InMemoryStatisticsStore with a custom clock (useful for testing).
   *
   * @param clock the clock to use for determining the current date
   */
  public InMemoryStatisticsStore(Clock clock) {
    this.clock = clock;
  }

  // ============================================================================
  // StatisticsWriter
  // ============================================================================

  @Override
  public void recordEvent(RedirectEvent event) {
    if (event == null || !config.isStatisticsEnabled()) {
      return;
    }

    String shortCode = event.shortCode();
    LocalDate date = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
    int hour = event.timestamp().atZone(ZoneOffset.UTC).getHour();

    // Store event
    events.computeIfAbsent(shortCode, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(date, k -> new CopyOnWriteArrayList<>())
        .add(event);

    // Update hourly aggregate
    hourlyAggregates.computeIfAbsent(shortCode, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(date, k -> new HourlyAggregate(date))
        .increment(hour);

    // Update daily aggregate
    dailyAggregates.computeIfAbsent(shortCode, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(date, k -> new DailyAggregate(date))
        .increment();

    logger().debug("Recorded event for shortCode={} date={} hour={}", shortCode, date, hour);
  }

  @Override
  public void flush() {
    // No-op for in-memory implementation (events are processed synchronously)
    logger().debug("flush() called - no-op for in-memory store");
  }

  @Override
  public long deleteEventsInRange(String shortCode, LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      return 0;
    }
    Set<String> targets = (shortCode == null)
        ? new HashSet<>(events.keySet())
        : Set.of(shortCode);

    long removed = 0;
    for (String code : targets) {
      var perDate = events.get(code);
      if (perDate == null) continue;

      LocalDate current = from;
      while (!current.isAfter(to)) {
        var bucket = perDate.remove(current);
        if (bucket != null) {
          removed += bucket.size();
        }
        current = current.plusDays(1);
      }
      if (perDate.isEmpty()) {
        events.remove(code);
      }
    }
    return removed;
  }

  // ============================================================================
  // StatisticsReader - Count queries
  // ============================================================================

  @Override
  public long getCountForDate(String shortCode, LocalDate date) {
    // First check daily aggregates
    var dailyMap = dailyAggregates.get(shortCode);
    if (dailyMap != null) {
      var daily = dailyMap.get(date);
      if (daily != null) {
        return daily.totalCount();
      }
    }

    // Fallback to hourly aggregates
    var hourlyMap = hourlyAggregates.get(shortCode);
    if (hourlyMap != null) {
      var hourly = hourlyMap.get(date);
      if (hourly != null) {
        return hourly.totalCount();
      }
    }

    return 0;
  }

  @Override
  public long getCountForDateRange(String shortCode, LocalDate from, LocalDate to) {
    long total = 0;
    LocalDate current = from;
    while (!current.isAfter(to)) {
      total += getCountForDate(shortCode, current);
      current = current.plusDays(1);
    }
    return total;
  }

  @Override
  public long getTotalCount(String shortCode) {
    long total = 0;

    var dailyMap = dailyAggregates.get(shortCode);
    if (dailyMap != null) {
      for (DailyAggregate daily : dailyMap.values()) {
        total += daily.totalCount();
      }
    }

    return total;
  }

  // ============================================================================
  // StatisticsReader - Aggregate queries
  // ============================================================================

  @Override
  public Optional<HourlyAggregate> getHourlyAggregate(String shortCode, LocalDate date) {
    if (!isInHotWindow(date)) {
      return Optional.empty();
    }

    var hourlyMap = hourlyAggregates.get(shortCode);
    if (hourlyMap == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(hourlyMap.get(date));
  }

  @Override
  public Optional<DailyAggregate> getDailyAggregate(String shortCode, LocalDate date) {
    var dailyMap = dailyAggregates.get(shortCode);
    if (dailyMap == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(dailyMap.get(date));
  }

  @Override
  public List<DailyAggregate> getDailyAggregates(String shortCode, LocalDate from, LocalDate to) {
    var dailyMap = dailyAggregates.get(shortCode);
    if (dailyMap == null) {
      return Collections.emptyList();
    }

    return dailyMap.values().stream()
        .filter(agg -> !agg.date().isBefore(from) && !agg.date().isAfter(to))
        .sorted(Comparator.comparing(DailyAggregate::date))
        .collect(Collectors.toList());
  }

  @Override
  public List<HourlyAggregate> getHourlyAggregates(String shortCode, LocalDate from, LocalDate to) {
    var hourlyMap = hourlyAggregates.get(shortCode);
    if (hourlyMap == null) {
      return Collections.emptyList();
    }

    LocalDate hotWindowStart = getHotWindowStart();

    return hourlyMap.values().stream()
        .filter(agg -> !agg.date().isBefore(from) && !agg.date().isAfter(to))
        .filter(agg -> !agg.date().isBefore(hotWindowStart))
        .sorted(Comparator.comparing(HourlyAggregate::date))
        .collect(Collectors.toList());
  }

  // ============================================================================
  // StatisticsReader - Event queries
  // ============================================================================

  @Override
  public List<RedirectEvent> getEventsForDate(String shortCode, LocalDate date) {
    var eventMap = events.get(shortCode);
    if (eventMap == null) {
      return Collections.emptyList();
    }

    var eventList = eventMap.get(date);
    if (eventList == null) {
      return Collections.emptyList();
    }

    return eventList.stream()
        .sorted(Comparator.comparing(RedirectEvent::timestamp))
        .collect(Collectors.toList());
  }

  @Override
  public List<RedirectEvent> getEventsForDateRange(String shortCode, LocalDate from, LocalDate to) {
    var eventMap = events.get(shortCode);
    if (eventMap == null) {
      return Collections.emptyList();
    }

    List<RedirectEvent> result = new ArrayList<>();
    LocalDate current = from;
    while (!current.isAfter(to)) {
      var eventList = eventMap.get(current);
      if (eventList != null) {
        result.addAll(eventList);
      }
      current = current.plusDays(1);
    }

    result.sort(Comparator.comparing(RedirectEvent::timestamp));
    return result;
  }

  @Override
  public List<LocalDate> getAvailableDates(String shortCode) {
    var dailyMap = dailyAggregates.get(shortCode);
    if (dailyMap == null) {
      return Collections.emptyList();
    }

    return dailyMap.keySet().stream()
        .sorted()
        .collect(Collectors.toList());
  }

  @Override
  public boolean isInHotWindow(LocalDate date) {
    LocalDate hotWindowStart = getHotWindowStart();
    return !date.isBefore(hotWindowStart);
  }

  @Override
  public Set<String> getKnownShortCodes() {
    Set<String> codes = new TreeSet<>();
    codes.addAll(events.keySet());
    codes.addAll(hourlyAggregates.keySet());
    codes.addAll(dailyAggregates.keySet());
    return codes;
  }

  // ============================================================================
  // StatisticsStore - Admin methods
  // ============================================================================

  @Override
  public StatisticsConfig getConfig() {
    return config;
  }

  @Override
  public void updateConfig(StatisticsConfig newConfig) {
    if (newConfig == null) {
      return;
    }

    config.setHotWindowDays(newConfig.hotWindowDays());
    config.setWriterBatchSize(newConfig.writerBatchSize());
    config.setAggregatorIntervalSeconds(newConfig.aggregatorIntervalSeconds());
    config.setStatisticsEnabled(newConfig.isStatisticsEnabled());

    logger().info("Updated statistics config: hotWindowDays={}, statisticsEnabled={}",
                  config.hotWindowDays(), config.isStatisticsEnabled());
  }

  @Override
  public void removeAllForShortCode(String shortCode) {
    events.remove(shortCode);
    hourlyAggregates.remove(shortCode);
    dailyAggregates.remove(shortCode);
    logger().info("Removed all statistics for shortCode={}", shortCode);
  }

  @Override
  public long reaggregate(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      return 0;
    }
    Set<String> codes = new HashSet<>(events.keySet());
    codes.addAll(hourlyAggregates.keySet());
    codes.addAll(dailyAggregates.keySet());

    long buckets = 0;
    for (String code : codes) {
      var perDate = events.get(code);
      var hourlyMap = hourlyAggregates.get(code);
      var dailyMap = dailyAggregates.get(code);

      LocalDate current = from;
      while (!current.isAfter(to)) {
        if (hourlyMap != null) hourlyMap.remove(current);
        if (dailyMap != null) dailyMap.remove(current);

        var bucket = (perDate != null) ? perDate.get(current) : null;
        if (bucket != null && !bucket.isEmpty()) {
          var hourly = hourlyAggregates
              .computeIfAbsent(code, k -> new ConcurrentHashMap<>())
              .computeIfAbsent(current, HourlyAggregate::new);
          var daily = dailyAggregates
              .computeIfAbsent(code, k -> new ConcurrentHashMap<>())
              .computeIfAbsent(current, DailyAggregate::new);
          for (RedirectEvent event : bucket) {
            int hour = event.timestamp().atZone(ZoneOffset.UTC).getHour();
            hourly.increment(hour);
            daily.increment();
          }
          buckets++;
        }
        current = current.plusDays(1);
      }
      if (hourlyMap != null && hourlyMap.isEmpty()) hourlyAggregates.remove(code);
      if (dailyMap != null && dailyMap.isEmpty()) dailyAggregates.remove(code);
    }
    logger().info("Reaggregated {} buckets in range [{}..{}]", buckets, from, to);
    return buckets;
  }

  @Override
  public void start() {
    // No-op for in-memory implementation (no background threads)
    logger().info("InMemoryStatisticsStore started (synchronous mode)");
  }

  @Override
  public void stop() {
    // No-op for in-memory implementation
    logger().info("InMemoryStatisticsStore stopped");
  }

  // ============================================================================
  // Test helper methods
  // ============================================================================

  /**
   * Clears all stored data. Useful for test setup/teardown.
   */
  public void clear() {
    events.clear();
    hourlyAggregates.clear();
    dailyAggregates.clear();
    logger().debug("Cleared all statistics data");
  }

  /**
   * Returns the number of shortCodes with recorded events.
   */
  public int getShortCodeCount() {
    return dailyAggregates.size();
  }

  /**
   * Returns the total number of events stored.
   */
  public long getTotalEventCount() {
    return events.values().stream()
        .flatMap(dateMap -> dateMap.values().stream())
        .mapToLong(List::size)
        .sum();
  }

  // ============================================================================
  // Internal helpers
  // ============================================================================

  private LocalDate getHotWindowStart() {
    return LocalDate.now(clock).minusDays(config.hotWindowDays());
  }

  @Override
  public Map<String, Object> getDebugInfo() {
    var info = new LinkedHashMap<String, Object>();

    // Config info
    info.put("statisticsEnabled", config.isStatisticsEnabled());
    info.put("hotWindowDays", config.hotWindowDays());
    info.put("writerBatchSize", config.writerBatchSize());
    info.put("aggregatorIntervalSeconds", config.aggregatorIntervalSeconds());

    // Store type
    info.put("storeType", "InMemory");

    // Storage info - events
    info.put("events_shortCodeCount", events.size());
    if (!events.isEmpty()) {
      var eventsDetail = new LinkedHashMap<String, Object>();
      for (var entry : events.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new LinkedHashMap<String, Integer>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey().toString(),
                           dateEntry.getValue() != null ? dateEntry.getValue().size() : 0);
          }
          eventsDetail.put(shortCode, dateDetail);
        }
      }
      info.put("events_detail", eventsDetail);
    }

    // Storage info - hourlyAggregates
    info.put("hourlyAggregates_shortCodeCount", hourlyAggregates.size());
    if (!hourlyAggregates.isEmpty()) {
      var hourlyDetail = new LinkedHashMap<String, Object>();
      for (var entry : hourlyAggregates.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new LinkedHashMap<String, Long>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey().toString(),
                           dateEntry.getValue() != null ? dateEntry.getValue().totalCount() : 0L);
          }
          hourlyDetail.put(shortCode, dateDetail);
        }
      }
      info.put("hourlyAggregates_detail", hourlyDetail);
    }

    // Storage info - dailyAggregates
    info.put("dailyAggregates_shortCodeCount", dailyAggregates.size());
    if (!dailyAggregates.isEmpty()) {
      var dailyDetail = new LinkedHashMap<String, Object>();
      for (var entry : dailyAggregates.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new LinkedHashMap<String, Long>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey().toString(),
                           dateEntry.getValue() != null ? dateEntry.getValue().totalCount() : 0L);
          }
          dailyDetail.put(shortCode, dateDetail);
        }
      }
      info.put("dailyAggregates_detail", dailyDetail);
    }

    // Summary counts
    info.put("totalEventCount", getTotalEventCount());
    info.put("shortCodeCount", getShortCodeCount());

    return info;
  }
}