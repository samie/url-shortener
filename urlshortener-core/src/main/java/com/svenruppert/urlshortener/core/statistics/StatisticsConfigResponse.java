package com.svenruppert.urlshortener.core.statistics;





/**
 * Response DTO for statistics configuration queries.
 */
public record StatisticsConfigResponse(
    int hotWindowDays,
    int writerBatchSize,
    int aggregatorIntervalSeconds,
    boolean statisticsEnabled
) {
  /**
   * Creates a response from a StatisticsConfig.
   */
  public static StatisticsConfigResponse from(StatisticsConfig config) {
    return new StatisticsConfigResponse(
        config.hotWindowDays(),
        config.writerBatchSize(),
        config.aggregatorIntervalSeconds(),
        config.isStatisticsEnabled()
    );
  }
}