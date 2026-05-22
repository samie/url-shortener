package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.vaadin.flow.server.VaadinSession;

import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Handles supported locales and session-based locale storage.
 * <p>
 * Supported languages:
 * - German (de)
 * - English (en)
 * - Finnish (fi)
 */
public final class LocaleSelection {

  public static final Locale DE = Locale.GERMAN;          // de
  public static final Locale EN = Locale.ENGLISH;         // en
  public static final Locale FI = new Locale("fi");       // fi
  public static final List<Locale> SUPPORTED = List.of(DE, EN, FI);
  private static final String SESSION_KEY = LocaleSelection.class.getName() + ".LOCALE";

  private LocaleSelection() {
    // utility class
  }

  /**
   * Matches a requested locale to the supported ones.
   * Only language is evaluated (country is ignored).
   */
  public static Locale match(Locale requested) {
    if (requested == null) {
      return EN;
    }

    String language = requested.getLanguage();

    return SUPPORTED.stream()
        .filter(l -> Objects.equals(l.getLanguage(), language))
        .findFirst()
        .orElse(EN);
  }

  /**
   * Returns locale from session or null if none set.
   */
  public static Locale getFromSession(VaadinSession session) {
    if (session == null) {
      return null;
    }
    Object value = session.getAttribute(SESSION_KEY);
    return (value instanceof Locale) ? (Locale) value : null;
  }

  /**
   * Stores locale in session.
   */
  public static void setToSession(VaadinSession session, Locale locale) {
    if (session == null || locale == null) {
      return;
    }
    session.setAttribute(SESSION_KEY, locale);
  }

  /**
   * Convenience method:
   * Returns session locale if present,
   * otherwise matches browser locale and stores it.
   */
  public static Locale resolveAndStore(VaadinSession session, Locale browserLocale) {
    Locale existing = getFromSession(session);
    if (existing != null) {
      return existing;
    }

    Locale effective = match(browserLocale);
    setToSession(session, effective);
    return effective;
  }
}