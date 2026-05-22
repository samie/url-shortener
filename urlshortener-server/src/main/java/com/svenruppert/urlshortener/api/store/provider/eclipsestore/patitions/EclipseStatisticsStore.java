package com.svenruppert.urlshortener.api.store.provider.eclipsestore.patitions;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.store.provider.eclipsestore.DataRoot;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsStore;
import com.svenruppert.urlshortener.core.statistics.DailyAggregate;
import com.svenruppert.urlshortener.core.statistics.HourlyAggregate;
import com.svenruppert.urlshortener.core.statistics.RedirectEvent;
import com.svenruppert.urlshortener.core.statistics.StatisticsConfig;
import org.eclipse.store.storage.types.StorageManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EclipseStore implementation of the StatisticsStore.
 * Uses a background thread for async event writing and provides
 * efficient queries through pre-computed aggregates.
 */
public class EclipseStatisticsStore
    implements StatisticsStore, HasLogger {

  private final StorageManager storage;
  private final Clock clock;
  private final BlockingQueue<RedirectEvent> eventQueue;
  private final AtomicBoolean running;

  private Thread writerThread;
  private Thread aggregatorThread;

  public EclipseStatisticsStore(StorageManager storage, Clock clock) {
    this.storage = storage;
    this.clock = clock != null ? clock : Clock.systemUTC();
    this.eventQueue = new LinkedBlockingQueue<>();
    this.running = new AtomicBoolean(false);
  }

  public EclipseStatisticsStore(StorageManager storage) {
    this(storage, Clock.systemUTC());
  }

  private DataRoot dataRoot() {
    return (DataRoot) storage.root();
  }

  // ==================== StatisticsWriter ====================

  @Override
  public void recordEvent(RedirectEvent event) {
    if (!getConfig().isStatisticsEnabled()) {
      logger().debug("Statistics disabled, ignoring event for shortCode={}", event.shortCode());
      return;
    }
    // Non-blocking add to queue
    if (!eventQueue.offer(event)) {
      logger().warn("Event queue is full, dropping event for shortCode={}", event.shortCode());
    } else {
      logger().info("Queued redirect event for shortCode={}, queue size={}", event.shortCode(), eventQueue.size());
    }
  }

  @Override
  public void flush() {
    logger().info("Flushing event queue, size={}", eventQueue.size());
    List<RedirectEvent> batch = new ArrayList<>();
    eventQueue.drainTo(batch);
    if (!batch.isEmpty()) {
      processBatch(batch);
    }
  }

  @Override
  public long deleteEventsInRange(String shortCode, LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      return 0;
    }
    flush();
    Set<String> targets = (shortCode == null)
        ? new HashSet<>(dataRoot().redirectEvents().keySet())
        : Set.of(shortCode);

    long removed = 0;
    boolean structureChanged = false;
    for (String code : targets) {
      var perDate = dataRoot().redirectEvents().get(code);
      if (perDate == null) continue;

      LocalDate current = from;
      while (!current.isAfter(to)) {
        var bucket = perDate.remove(current.toString());
        if (bucket != null) {
          removed += bucket.size();
          structureChanged = true;
        }
        current = current.plusDays(1);
      }
      if (perDate.isEmpty()) {
        dataRoot().redirectEvents().remove(code);
      } else {
        storage.store(perDate);
      }
    }

    if (structureChanged) {
      storage.store(dataRoot().redirectEvents());
      logger().info("Deleted {} events in range [{}..{}] for shortCode={}",
                    removed, from, to, shortCode == null ? "<all>" : shortCode);
    }
    return removed;
  }

  // ==================== StatisticsReader ====================

  @Override
  public long getCountForDate(String shortCode, LocalDate date) {
    // First check daily aggregate
    logger().info("getCountForDate for shortCode {} and date {}", shortCode, date);
    var dailyAggregates = dataRoot().dailyAggregates();
    logger().info("dailyAggregates Map - keySet {}", dailyAggregates.keySet());
    var dailyMap = dailyAggregates.get(shortCode);
    logger().info("getCountForDate - dailyMap {}", dailyMap);
    if (dailyMap != null) {
      var aggregate = dailyMap.get(date.toString());
      if (aggregate != null) {
        return aggregate.totalCount();
      }
    }

    // Fall back to hourly aggregate if in hot window
    if (isInHotWindow(date)) {
      logger().info("isInHotWindow TRUE");
      var hourlyMap = dataRoot().hourlyAggregates().get(shortCode);
      logger().info("hourlyMap {}", hourlyMap);
      if (hourlyMap != null) {
        logger().info("hourlyMap != null");
        var hourly = hourlyMap.get(date.toString());
        if (hourly != null) {
          return hourly.totalCount();
        }
      } else {
        logger().info("hourlyMap == null");
      }
    }
    logger().info("default return value 0");
    return 0;
  }

  @Override
  public long getCountForDateRange(String shortCode, LocalDate from, LocalDate to) {
    logger().info("getCountForDateRange shortCode {}", shortCode);
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

    // Sum all daily aggregates
    var dailyMap = dataRoot().dailyAggregates().get(shortCode);
    if (dailyMap != null) {
      for (DailyAggregate aggregate : dailyMap.values()) {
        total += aggregate.totalCount();
      }
    }

    // Add hourly aggregates for dates not yet in daily
    var hourlyMap = dataRoot().hourlyAggregates().get(shortCode);
    if (hourlyMap != null) {
      for (Map.Entry<String, HourlyAggregate> entry : hourlyMap.entrySet()) {
        if (dailyMap == null || !dailyMap.containsKey(entry.getKey())) {
          total += entry.getValue().totalCount();
        }
      }
    }

    return total;
  }

  @Override
  public Optional<HourlyAggregate> getHourlyAggregate(String shortCode, LocalDate date) {
    if (!isInHotWindow(date)) {
      return Optional.empty();
    }
    var hourlyMap = dataRoot().hourlyAggregates().get(shortCode);
    if (hourlyMap == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(hourlyMap.get(date.toString()));
  }

  @Override
  public Optional<DailyAggregate> getDailyAggregate(String shortCode, LocalDate date) {
    var dailyMap = dataRoot().dailyAggregates().get(shortCode);
    if (dailyMap == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(dailyMap.get(date.toString()));
  }

  @Override
  public List<DailyAggregate> getDailyAggregates(String shortCode, LocalDate from, LocalDate to) {
    List<DailyAggregate> result = new ArrayList<>();
    var dailyMap = dataRoot().dailyAggregates().get(shortCode);
    if (dailyMap == null) {
      return result;
    }

    LocalDate current = from;
    while (!current.isAfter(to)) {
      var aggregate = dailyMap.get(current.toString());
      if (aggregate != null) {
        result.add(aggregate);
        logger().info("getDailyAggregates - added aggregate {} for date {}", aggregate, current);
      } else {
        logger().info("getDailyAggregates - aggregate is null for date {}", current);
      }
      current = current.plusDays(1);
    }
    return result;
  }

  @Override
  public List<HourlyAggregate> getHourlyAggregates(String shortCode, LocalDate from, LocalDate to) {
    List<HourlyAggregate> result = new ArrayList<>();
    var hourlyMap = dataRoot().hourlyAggregates().get(shortCode);
    if (hourlyMap == null) {
      return result;
    }

    LocalDate hotWindowStart = getHotWindowStart();
    LocalDate effectiveFrom = from.isBefore(hotWindowStart) ? hotWindowStart : from;

    LocalDate current = effectiveFrom;
    while (!current.isAfter(to)) {
      var aggregate = hourlyMap.get(current.toString());
      if (aggregate != null) {
        result.add(aggregate);
      }
      current = current.plusDays(1);
    }
    return result;
  }

  @Override
  public List<RedirectEvent> getEventsForDate(String shortCode, LocalDate date) {
    var eventMap = dataRoot().redirectEvents().get(shortCode);
    if (eventMap == null) {
      return Collections.emptyList();
    }
    var events = eventMap.get(date.toString());
    if (events == null) {
      return Collections.emptyList();
    }
    // Return sorted copy
    List<RedirectEvent> sorted = new ArrayList<>(events);
    sorted.sort(Comparator.comparing(RedirectEvent::timestamp));
    return sorted;
  }

  @Override
  public List<RedirectEvent> getEventsForDateRange(String shortCode, LocalDate from, LocalDate to) {
    List<RedirectEvent> result = new ArrayList<>();
    LocalDate current = from;
    while (!current.isAfter(to)) {
      result.addAll(getEventsForDate(shortCode, current));
      current = current.plusDays(1);
    }
    result.sort(Comparator.comparing(RedirectEvent::timestamp));
    return result;
  }

  @Override
  public List<LocalDate> getAvailableDates(String shortCode) {
    Set<LocalDate> dates = new TreeSet<>();

    var eventMap = dataRoot().redirectEvents().get(shortCode);
    if (eventMap != null) {
      eventMap.keySet().stream().map(LocalDate::parse).forEach(dates::add);
    }

    var dailyMap = dataRoot().dailyAggregates().get(shortCode);
    if (dailyMap != null) {
      dailyMap.keySet().stream().map(LocalDate::parse).forEach(dates::add);
    }

    return new ArrayList<>(dates);
  }

  @Override
  public boolean isInHotWindow(LocalDate date) {
    LocalDate hotWindowStart = getHotWindowStart();
    LocalDate today = LocalDate.now(clock);
    return !date.isBefore(hotWindowStart) && !date.isAfter(today);
  }

  @Override
  public Set<String> getKnownShortCodes() {
    Set<String> codes = new TreeSet<>();
    codes.addAll(dataRoot().redirectEvents().keySet());
    codes.addAll(dataRoot().hourlyAggregates().keySet());
    codes.addAll(dataRoot().dailyAggregates().keySet());
    return codes;
  }

  // ==================== StatisticsStore ====================

  @Override
  public StatisticsConfig getConfig() {
    return dataRoot().statisticsConfig();
  }

  @Override
  public void updateConfig(StatisticsConfig config) {
    logger().info("Updating statistics config: {}", config);
    // Copy values to existing config object (EclipseStore tracks the object)
    var existing = dataRoot().statisticsConfig();
    existing.setHotWindowDays(config.hotWindowDays());
    existing.setWriterBatchSize(config.writerBatchSize());
    existing.setAggregatorIntervalSeconds(config.aggregatorIntervalSeconds());
    existing.setStatisticsEnabled(config.isStatisticsEnabled());
    storage.store(existing);
  }

  @Override
  public void removeAllForShortCode(String shortCode) {
    logger().info("Removing all statistics for shortCode={}", shortCode);
    dataRoot().removeStatisticsForShortCode(shortCode);
    storage.store(dataRoot().redirectEvents());
    storage.store(dataRoot().hourlyAggregates());
    storage.store(dataRoot().dailyAggregates());
  }

  @Override
  public long reaggregate(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      return 0;
    }
    flush();
    Set<String> codes = new HashSet<>(dataRoot().redirectEvents().keySet());
    codes.addAll(dataRoot().hourlyAggregates().keySet());
    codes.addAll(dataRoot().dailyAggregates().keySet());

    long buckets = 0;
    for (String code : codes) {
      var perDate = dataRoot().redirectEvents().get(code);
      var hourlyMap = dataRoot().hourlyAggregates().get(code);
      var dailyMap = dataRoot().dailyAggregates().get(code);

      LocalDate current = from;
      while (!current.isAfter(to)) {
        String key = current.toString();
        if (hourlyMap != null) hourlyMap.remove(key);
        if (dailyMap != null) dailyMap.remove(key);

        var bucket = (perDate != null) ? perDate.get(key) : null;
        if (bucket != null && !bucket.isEmpty()) {
          HourlyAggregate hourly = dataRoot().getOrCreateHourlyAggregate(code, current);
          DailyAggregate daily = dataRoot().getOrCreateDailyAggregate(code, current);
          for (RedirectEvent event : bucket) {
            int hour = event.timestamp().atZone(ZoneOffset.UTC).getHour();
            hourly.increment(hour);
            daily.increment();
          }
          storage.store(hourly);
          storage.store(daily);
          buckets++;
        }
        current = current.plusDays(1);
      }
      if (hourlyMap != null) storage.store(hourlyMap);
      if (dailyMap != null) storage.store(dailyMap);
    }
    storage.store(dataRoot().hourlyAggregates());
    storage.store(dataRoot().dailyAggregates());
    logger().info("Reaggregated {} buckets in range [{}..{}]", buckets, from, to);
    return buckets;
  }

  @Override
  public void start() {
    if (running.getAndSet(true)) {
      logger().warn("Statistics store already started");
      return;
    }

    // Log what was loaded from storage
    logger().info("Starting statistics store - loaded data:");
    logger().info("  - redirectEvents: {} shortCodes", dataRoot().redirectEvents().size());
    logger().info("  - hourlyAggregates: {} shortCodes", dataRoot().hourlyAggregates().size());
    logger().info("  - dailyAggregates: {} shortCodes", dataRoot().dailyAggregates().size());
    logger().info("  - statisticsEnabled: {}", getConfig().isStatisticsEnabled());

    logger().info("Starting statistics store background threads");

    writerThread = new Thread(this::writerLoop, "statistics-writer");
    writerThread.setDaemon(true);
    writerThread.start();

    aggregatorThread = new Thread(this::aggregatorLoop, "statistics-aggregator");
    aggregatorThread.setDaemon(true);
    aggregatorThread.start();
  }

  @Override
  public void stop() {
    if (!running.getAndSet(false)) {
      return;
    }

    logger().info("Stopping statistics store background threads");

    // Interrupt threads
    if (writerThread != null) {
      writerThread.interrupt();
    }
    if (aggregatorThread != null) {
      aggregatorThread.interrupt();
    }

    // Flush remaining events
    flush();

    // Log final state
    logger().info("Statistics store stopped - final data:");
    logger().info("  - redirectEvents: {} shortCodes", dataRoot().redirectEvents().size());
    logger().info("  - hourlyAggregates: {} shortCodes", dataRoot().hourlyAggregates().size());
    logger().info("  - dailyAggregates: {} shortCodes", dataRoot().dailyAggregates().size());
  }

  // ==================== Background Threads ====================

  private void writerLoop() {
    logger().info("Writer thread started");
    List<RedirectEvent> batch = new ArrayList<>();

    while (running.get()) {
      try {
        // Wait for first event
        RedirectEvent event = eventQueue.poll(1, TimeUnit.SECONDS);
        if (event == null) {
          continue;
        }

        batch.add(event);

        // Drain up to batch size
        int maxBatch = getConfig().writerBatchSize();
        eventQueue.drainTo(batch, maxBatch - 1);

        processBatch(batch);
        batch.clear();

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger().error("Error in writer loop", e);
      }
    }

    logger().info("Writer thread stopped");
  }

  private void processBatch(List<RedirectEvent> batch) {
    if (batch.isEmpty()) {
      return;
    }

    logger().info("Processing batch of {} events", batch.size());

    // Group events by shortCode and date
    Map<String, Map<LocalDate, List<RedirectEvent>>> grouped = new HashMap<>();
    for (RedirectEvent event : batch) {
      LocalDate date = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
      logger().info("processBatch - event.shortCode() = {}, event.timestamp() = {}", event.shortCode(), event.timestamp());
      grouped
          .computeIfAbsent(event.shortCode(), k -> new HashMap<>())
          .computeIfAbsent(date, k -> new ArrayList<>())
          .add(event);
    }

    // Collect all modified objects and maps
    //Set<Object> objectsToStore = new HashSet<>();

    for (var shortCodeEntry : grouped.entrySet()) {
      String shortCode = shortCodeEntry.getKey();

      for (var dateEntry : shortCodeEntry.getValue().entrySet()) {
        LocalDate date = dateEntry.getKey();
        List<RedirectEvent> events = dateEntry.getValue();

        Objects.requireNonNull(date);
        logger().info("processBatch - date .. {} - events... {}", date, events);

        // --- Redirect Events ---
        List<RedirectEvent> eventList = dataRoot().getOrCreateEventList(shortCode, date);
        eventList.addAll(events);
        events.forEach(storage::store);
        // Also store the inner map for this shortCode
        var redirectEvents = dataRoot().redirectEvents();
        var redirectEventsForShortCode = redirectEvents.get(shortCode);
        //objectsToStore.add(redirectEventsForShortCode);
        storage.store(redirectEventsForShortCode);

        // --- Hourly Aggregate ---
        HourlyAggregate hourly = dataRoot().getOrCreateHourlyAggregate(shortCode, date);
        for (RedirectEvent event : events) {
          int hour = event.timestamp().atZone(ZoneOffset.UTC).getHour();
          hourly.increment(hour);
        }
        //        objectsToStore.add(hourly);
        //        objectsToStore.add(dataRoot().hourlyAggregates().get(shortCode));
        storage.store(hourly);
        storage.store(dataRoot().hourlyAggregates().get(shortCode));

        // --- Daily Aggregate ---
        DailyAggregate daily = dataRoot().getOrCreateDailyAggregate(shortCode, date);
        daily.add(events.size());
        logger().info("processBatch - daily.add(events.size()) = {}", daily.totalCount());
        //        objectsToStore.add(daily);
        //        objectsToStore.add(dataRoot().dailyAggregates().get(shortCode));
        storage.store(daily);
        storage.store(dataRoot().dailyAggregates().get(shortCode));
        storage.store(dataRoot().dailyAggregates());

      }
    }

    // Persist all objects
    //    for (Object obj : objectsToStore) {
    //      storage.store(obj);
    //    }

    // Always persist the outer maps to ensure structural changes are saved
    storage.store(dataRoot().redirectEvents());
    storage.store(dataRoot().hourlyAggregates());
    storage.store(dataRoot().dailyAggregates());

    //    logger().info("Batch processing complete, stored {} objects + 3 outer maps", objectsToStore.size());
    logger().info("Batch processing complete, stored {} objects + outer maps", batch.size());
  }

  private void aggregatorLoop() {
    logger().info("Aggregator thread started");

    while (running.get()) {
      try {
        var statisticsConfig = getConfig();
        logger().info("Aggregator thread - statisticsConfig: {}", statisticsConfig);
        int intervalSeconds = statisticsConfig.aggregatorIntervalSeconds();
        TimeUnit.SECONDS.sleep(intervalSeconds);

        cleanupOldHourlyAggregates();

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger().error("Error in aggregator loop", e);
      }
    }

    logger().info("Aggregator thread stopped");
  }

  /**
   * Removes hourly aggregates that have fallen outside the hot window.
   */
  private void cleanupOldHourlyAggregates() {
    LocalDate hotWindowStart = getHotWindowStart();
    logger().debug("Cleaning up hourly aggregates before {}", hotWindowStart);

    int removed = 0;
    for (var hourlyMap : dataRoot().hourlyAggregates().values()) {
      Iterator<String> it = hourlyMap.keySet().iterator();
      while (it.hasNext()) {
        LocalDate date = LocalDate.parse(it.next());
        if (date.isBefore(hotWindowStart)) {
          it.remove();
          removed++;
        }
      }
    }

    if (removed > 0) {
      logger().info("Removed {} old hourly aggregates", removed);
      storage.store(dataRoot().hourlyAggregates());
    }
  }

  private LocalDate getHotWindowStart() {
    int hotWindowDays = getConfig().hotWindowDays();
    return LocalDate.now(clock).minusDays(hotWindowDays - 1);
  }

  @Override
  public java.util.Map<String, Object> getDebugInfo() {
    var info = new java.util.LinkedHashMap<String, Object>();

    // Config info
    var config = getConfig();
    info.put("statisticsEnabled", config.isStatisticsEnabled());
    info.put("hotWindowDays", config.hotWindowDays());
    info.put("writerBatchSize", config.writerBatchSize());
    info.put("aggregatorIntervalSeconds", config.aggregatorIntervalSeconds());

    // Queue info
    info.put("eventQueueSize", eventQueue.size());
    info.put("writerThreadRunning", running.get());

    // Storage info - redirectEvents
    var redirectEvents = dataRoot().redirectEvents();
    info.put("redirectEvents_shortCodeCount", redirectEvents != null ? redirectEvents.size() : "NULL");
    if (redirectEvents != null && !redirectEvents.isEmpty()) {
      var eventsDetail = new java.util.LinkedHashMap<String, Object>();
      for (var entry : redirectEvents.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new java.util.LinkedHashMap<String, Integer>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey(),
                           dateEntry.getValue() != null ? dateEntry.getValue().size() : 0);
          }
          eventsDetail.put(shortCode, dateDetail);
        }
      }
      info.put("redirectEvents_detail", eventsDetail);
    }

    // Storage info - hourlyAggregates
    var hourlyAggregates = dataRoot().hourlyAggregates();
    info.put("hourlyAggregates_shortCodeCount", hourlyAggregates != null ? hourlyAggregates.size() : "NULL");
    if (hourlyAggregates != null && !hourlyAggregates.isEmpty()) {
      var hourlyDetail = new java.util.LinkedHashMap<String, Object>();
      for (var entry : hourlyAggregates.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new java.util.LinkedHashMap<String, Long>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey(),
                           dateEntry.getValue() != null ? dateEntry.getValue().totalCount() : 0L);
          }
          hourlyDetail.put(shortCode, dateDetail);
        }
      }
      info.put("hourlyAggregates_detail", hourlyDetail);
    }

    // Storage info - dailyAggregates
    var dailyAggregates = dataRoot().dailyAggregates();
    info.put("dailyAggregates_shortCodeCount", dailyAggregates != null ? dailyAggregates.size() : "NULL");
    if (dailyAggregates != null && !dailyAggregates.isEmpty()) {
      var dailyDetail = new java.util.LinkedHashMap<String, Object>();
      for (var entry : dailyAggregates.entrySet()) {
        String shortCode = entry.getKey();
        var dateMap = entry.getValue();
        if (dateMap != null) {
          var dateDetail = new java.util.LinkedHashMap<String, Long>();
          for (var dateEntry : dateMap.entrySet()) {
            dateDetail.put(dateEntry.getKey(),
                           dateEntry.getValue() != null ? dateEntry.getValue().totalCount() : 0L);
          }
          dailyDetail.put(shortCode, dateDetail);
        }
      }
      info.put("dailyAggregates_detail", dailyDetail);
    }

    return info;
  }
}
