package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;

public final class I18n
    implements HasLogger {

  private I18n() {
  }

  public static String tr(String key, String fallback, Object... params) {
    HasLogger.staticLogger().info("I18n.tr - key '{}' - fallback '{}' - params '{}'", key, fallback, params);
    var other = fallback != null ? fallback : key;
    HasLogger.staticLogger().info("I18n.tr - other '{}'", other);
    String pattern = lookup(key).orElse(other);
    if (params == null || params.length == 0) return pattern;
    HasLogger.staticLogger().info("I18n.tr - pattern '{}'", pattern);
    var formatted = MessageFormat.format(pattern, params);
    HasLogger.staticLogger().info("I18n.tr - formatted '{}'", formatted);
    return formatted;
  }

  private static Optional<String> lookup(String key) {
    try {
      I18NProvider provider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
      Locale locale = currentLocale();
      HasLogger.staticLogger().info("I18n.tr - lookup - locale '{}'", locale);
      //      String t = provider.getTranslation(key, locale, List.of());
      String t = provider.getTranslation(key, locale);
      HasLogger.staticLogger().info("I18n.tr - lookup - translated '{}'", t);
      if (t == null || t.isBlank() || t.equals(key)) return Optional.empty();
      return Optional.of(t);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private static Locale currentLocale() {
    UI ui = UI.getCurrent();
    if (ui != null && ui.getLocale() != null) return ui.getLocale();
    var req = VaadinService.getCurrentRequest();
    if (req != null && req.getLocale() != null) return req.getLocale();
    return Locale.getDefault();
  }
}