package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.ColumnVisibilityClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ColumnVisibilityService
    implements HasLogger {

  private final ColumnVisibilityClient client;
  private final String userId;
  private final String viewId;

  public ColumnVisibilityService(
      ColumnVisibilityClient client,
      String userId,
      String viewId) {
    this.client = Objects.requireNonNull(client);
    this.userId = Objects.requireNonNull(userId);
    this.viewId = Objects.requireNonNull(viewId);
  }

  /**
   * Lädt Serverwerte, fällt bei Fehlern auf leere Map zurück.
   */
  public Map<String, Boolean> loadOrEmpty() {
    try {
      return client.load(userId, viewId);
    } catch (Exception e) {
      logger().warn("Load column visibilities failed: {}", e.toString());
      return Map.of();
    }
  }

  /**
   * Liefert eine Map, die alle bekannten Keys enthält (Default true), überlagert mit Serverwerten.
   */
  public Map<String, Boolean> mergeWithDefaults(Iterable<String> knownKeys) {
    Map<String, Boolean> merged = new LinkedHashMap<>();
    for (String k : knownKeys) merged.put(k, Boolean.TRUE);
    var server = loadOrEmpty();
    server.forEach((k, v) -> merged.put(k, v != null ? v : Boolean.TRUE));
    return merged;
  }

  /**
   * Persistiert eine einzelne Änderung (idempotent).
   */
  public void setSingle(String columnKey, boolean visible) {
    try {
      client.editSingle(userId, viewId, columnKey, visible);
    } catch (IOException | InterruptedException e) {
      logger().warn("Persist single failed for key {}: {}", columnKey, e.toString());
    }
  }

  /**
   * Persistiert mehrere Änderungen in einem Rutsch.
   */
  public void setBulk(Map<String, Boolean> changes) {
    if (changes == null || changes.isEmpty()) return;
    try {
      client.editBulk(userId, viewId, changes);
    } catch (IOException | InterruptedException e) {
      logger().warn("Persist bulk failed {}: {}", changes.keySet(), e.toString());
    }
  }
}