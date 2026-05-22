package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.vaadin.flow.component.Component;

public interface I18nSupport {

  default String tr(String key) {
    return ((Component) this).getTranslation(key);
  }

  default String tr(String key, String fallback) {
    final String translated = ((Component) this).getTranslation(key);
    if (translated == null || translated.isBlank() || translated.equals(key)) {
      return fallback;
    }
    return translated;
  }

  default String tr(String key, String fallback, Object... params) {
    final String translated = ((Component) this).getTranslation(key, params);
    if (translated == null || translated.isBlank() || translated.equals(key)) {
      // fallback uses the same placeholder style as Vaadin (MessageFormat)
      return java.text.MessageFormat.format(fallback, params);
    }
    return translated;
  }
}