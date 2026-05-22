package com.svenruppert.urlshortener.ui.vaadin.views.statistics;



/**
 * Defines the granularity for statistics aggregation.
 */
public enum AggregationGranularity {
  HOUR("hour"),
  DAY("day"),
  WEEK("week"),
  MONTH("month");

  private final String key;

  AggregationGranularity(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}