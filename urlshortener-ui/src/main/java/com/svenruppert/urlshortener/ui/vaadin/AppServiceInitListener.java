package com.svenruppert.urlshortener.ui.vaadin;



import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.AppUser;
import com.svenruppert.urlshortener.ui.vaadin.security.LoginConfig;
import com.svenruppert.urlshortener.ui.vaadin.tools.LocaleSelection;
import com.svenruppert.vaadin.security.authorization.api.SessionAccessor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;

import java.util.Locale;
import java.util.Set;

/**
 * Vaadin 25 ServiceInitListener
 * - Browser-Locale als Default
 * - Beschränkt auf DE / EN / FI
 * - Session-basierte Sprachwahl
 */
public class AppServiceInitListener implements VaadinServiceInitListener {

  @Override
  public void serviceInit(ServiceInitEvent event) {

    event.getSource().addUIInitListener(uiEvent -> {

      UI ui = uiEvent.getUI();
      VaadinSession session = ui.getSession();

      if (!LoginConfig.isLoginEnabled()
          && SessionAccessor.<AppUser>currentSubject().isAbsent()) {
        SessionAccessor.setCurrentSubject(new AppUser("guest", Set.of(AppRole.USER)));
      }

      // 1) Falls Benutzer in Session bereits Sprache gewählt hat
      Locale sessionLocale = LocaleSelection.getFromSession(session);
      if (sessionLocale != null) {
        ui.setLocale(sessionLocale);
        session.setLocale(sessionLocale);
        return;
      }

      // 2) Sonst Browser-Locale verwenden (Accept-Language)
      Locale browserLocale = ui.getLocale();
      Locale effective = LocaleSelection.match(browserLocale);

      ui.setLocale(effective);
      session.setLocale(effective);
      LocaleSelection.setToSession(session, effective);
    });
  }
}