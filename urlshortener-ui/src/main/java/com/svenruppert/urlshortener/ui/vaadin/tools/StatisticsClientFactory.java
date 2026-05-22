package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.svenruppert.urlshortener.client.StatisticsClient;

public class StatisticsClientFactory {

  private StatisticsClientFactory() {
  }

  public static StatisticsClient newInstance() {
    return new StatisticsClient();
  }
}