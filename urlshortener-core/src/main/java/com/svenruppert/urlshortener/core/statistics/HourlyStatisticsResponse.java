package com.svenruppert.urlshortener.core.statistics;





import java.time.LocalDate;

/**
 * Response DTO for hourly statistics queries.
 * Contains the 24-hour breakdown for a single day.
 */
public record HourlyStatisticsResponse(
    String shortCode,
    LocalDate date,
    long[] hourlyCounts,
    long totalCount,
    int peakHour
) {
  /**
   * Creates a response from an HourlyAggregate.
   */
  public static HourlyStatisticsResponse from(String shortCode, HourlyAggregate aggregate) {
    return new HourlyStatisticsResponse(
        shortCode,
        aggregate.date(),
        aggregate.hourlyCounts(),
        aggregate.totalCount(),
        aggregate.peakHour()
    );
  }
}