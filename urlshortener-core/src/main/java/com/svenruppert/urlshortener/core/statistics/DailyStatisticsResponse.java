package com.svenruppert.urlshortener.core.statistics;





import java.time.LocalDate;

/**
 * Response DTO for daily statistics queries.
 */
public record DailyStatisticsResponse(
    String shortCode,
    LocalDate date,
    long totalCount,
    long uniqueVisitors
) {
  /**
   * Creates a response from a DailyAggregate.
   */
  public static DailyStatisticsResponse from(String shortCode, DailyAggregate aggregate) {
    return new DailyStatisticsResponse(
        shortCode,
        aggregate.date(),
        aggregate.totalCount(),
        aggregate.uniqueVisitors()
    );
  }
}