package com.svenruppert.urlshortener.ui.vaadin.events;



import com.svenruppert.dependencies.core.logger.HasLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class StoreEvents
    implements HasLogger {

  private static final List<Consumer<StoreConnectionChanged>> LISTENERS = new CopyOnWriteArrayList<>();

  private StoreEvents() {
  }

  public static AutoCloseable subscribe(Consumer<StoreConnectionChanged> listener) {
    LISTENERS.add(listener);
    return () -> LISTENERS.remove(listener);
  }

  public static void publish(StoreConnectionChanged evt) {
    for (var l : LISTENERS) l.accept(evt);
  }

}