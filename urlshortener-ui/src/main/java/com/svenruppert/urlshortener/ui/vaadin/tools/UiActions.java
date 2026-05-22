package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.vaadin.flow.component.UI;

public class UiActions
    implements HasLogger {

  private UiActions() {
  }

  public static void copyToClipboard(String value) {
    HasLogger.staticLogger().info("copyToClipboard {}", value);
    UI.getCurrent()
        .getPage()
        .executeJs("navigator.clipboard.writeText($0)", value);
  }
}