package com.svenruppert.urlshortener.core.statistics;





import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for statistics timeline queries.
 * Contains daily counts over a date range.
 */
public record StatisticsTimelineResponse(
    String shortCode,
    LocalDate from,
    LocalDate to,
    List<DailyCount> dailyCounts,
    long totalCount
) {
  /**
   * Calculates the total count from the daily counts.
   */
  public static StatisticsTimelineResponse create(
      String shortCode,
      LocalDate from,
      LocalDate to,
      List<DailyCount> dailyCounts
  ) {
    long total = dailyCounts.stream().mapToLong(DailyCount::count).sum();
    return new StatisticsTimelineResponse(shortCode, from, to, dailyCounts, total);
  }

  /**
   * A single day's count in the timeline.
   */
  public record DailyCount(LocalDate date, long count) { }
}