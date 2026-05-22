package com.svenruppert.urlshortener.ui.vaadin.components;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.AdminClient;
import com.svenruppert.urlshortener.core.StoreInfo;
import com.svenruppert.urlshortener.ui.vaadin.events.StoreConnectionChanged;
import com.svenruppert.urlshortener.ui.vaadin.events.StoreEvents;
import com.svenruppert.urlshortener.ui.vaadin.events.StoreMode;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.Objects;

/**
 * Zeigt an, ob die Anwendung aktuell InMemory oder EclipseStore verwendet.
 * Nutzt Farbcodierung (grün = persistent, blau = volatil, rot = Fehler).
 */
@CssImport("./styles/store-indicator.css")
public class StoreIndicator
    extends HorizontalLayout
    implements HasLogger, I18nSupport {

  // CSS classes
  private static final String CLASS_ROOT = "store-indicator";
  private static final String CLASS_ICON = "store-indicator__icon";
  private static final String CLASS_BADGE = "store-indicator__badge";
  private static final String CLASS_DETAILS = "store-indicator__details";
  private static final String STATE_ECLIPSE = "store-indicator--eclipse";
  private static final String STATE_MEMORY = "store-indicator--memory";
  private static final String STATE_DOWN = "store-indicator--unavailable";
  // i18n keys
  private static final String NS = "storeIndicator";
  private static final String K_BADGE_ECLIPSE = NS + ".badge.eclipse";
  private static final String K_BADGE_MEMORY = NS + ".badge.memory";
  private static final String K_BADGE_UNAVAILABLE = NS + ".badge.unavailable";
  private static final String K_DETAILS_ITEMS = NS + ".details.items"; // "· {0} items"
  private static final String K_TITLE_ECLIPSE = NS + ".title.eclipse";
  private static final String K_TITLE_MEMORY = NS + ".title.memory";
  private static final String K_TITLE_UNAVAILABLE = NS + ".title.unavailable";
  private final AdminClient adminClient;
  private final Icon dbIcon = VaadinIcon.DATABASE.create();
  private final Span badge = new Span();
  private final Span details = new Span();
  private StoreMode lastMode = StoreMode.UNAVAILABLE;

  public StoreIndicator(AdminClient adminClient) {
    this.adminClient = adminClient;

    // --- Layoutgrundlage ---
    setAlignItems(FlexComponent.Alignment.CENTER);
    setSpacing(true);
    setPadding(false);

    addClassName(CLASS_ROOT);
    dbIcon.addClassName(CLASS_ICON);
    badge.addClassName(CLASS_BADGE);
    details.addClassName(CLASS_DETAILS);

    add(dbIcon, badge, details);
    applyState(StoreMode.UNAVAILABLE);
    applyTexts(StoreMode.UNAVAILABLE, 0);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    refreshOnce();
    UI ui = attachEvent.getUI();
    ui.setPollInterval(10000); // alle 10 Sekunden aktualisieren
    ui.addPollListener(_ -> refreshOnce());
  }

  /**
   * Holt die StoreInfo und aktualisiert das UI.
   */
  public void refreshOnce() {
    getUI().ifPresent(ui -> ui.access(() -> {
      try {
        StoreInfo info = adminClient.getStoreInfo();
        boolean persistent = "EclipseStore".equalsIgnoreCase(info.mode());

        var newMode = persistent ? StoreMode.ECLIPSE_STORE : StoreMode.IN_MEMORY;

        applyTexts(newMode, info.mappings());
        applyState(newMode);

        if (newMode != lastMode) {
          lastMode = newMode;
          StoreEvents.publish(new StoreConnectionChanged(newMode, info.mappings()));
        }

      } catch (Exception e) {
        applyTexts(StoreMode.UNAVAILABLE, 0);
        applyState(StoreMode.UNAVAILABLE);

        if (lastMode != StoreMode.UNAVAILABLE) {
          lastMode = StoreMode.UNAVAILABLE;
          StoreEvents.publish(new StoreConnectionChanged(StoreMode.UNAVAILABLE, 0));
        }
      }
    }));
  }

  private void applyTexts(StoreMode mode, long mappings) {
    Objects.requireNonNull(mode, "mode");

    switch (mode) {
      case ECLIPSE_STORE -> {
        badge.setText(tr(K_BADGE_ECLIPSE, "EclipseStore"));
        details.setText(tr(K_DETAILS_ITEMS, "· {0} items", mappings));
        getElement().setAttribute("title", tr(K_TITLE_ECLIPSE, "Persistent via EclipseStore"));
      }
      case IN_MEMORY -> {
        badge.setText(tr(K_BADGE_MEMORY, "InMemory"));
        details.setText(tr(K_DETAILS_ITEMS, "· {0} items", mappings));
        getElement().setAttribute("title", tr(K_TITLE_MEMORY, "Volatile (InMemory)"));
      }
      default -> {
        badge.setText(tr(K_BADGE_UNAVAILABLE, "Unavailable"));
        details.setText("");
        getElement().setAttribute("title", tr(K_TITLE_UNAVAILABLE, "StoreInfo endpoint unavailable"));
      }
    }
  }

  private void applyState(StoreMode mode) {
    // nur die drei Zustandsklassen managen
    removeClassNames(STATE_ECLIPSE, STATE_MEMORY, STATE_DOWN);

    if (Objects.requireNonNull(mode) == StoreMode.ECLIPSE_STORE) {
      addClassName(STATE_ECLIPSE);
    } else if (mode == StoreMode.IN_MEMORY) {
      addClassName(STATE_MEMORY);
    } else if (mode == StoreMode.UNAVAILABLE) {
      addClassName(STATE_DOWN);
    }
  }
}