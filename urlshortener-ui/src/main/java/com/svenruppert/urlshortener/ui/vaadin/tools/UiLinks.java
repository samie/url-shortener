package com.svenruppert.urlshortener.ui.vaadin.tools;



import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Factory helpers for link components.
 * HTTP links are flagged with a visible warning icon so users immediately see
 * that the target URL is not encrypted.
 */
public final class UiLinks {

  private UiLinks() {
  }

  /**
   * Creates an anchor for {@code url}.  If the URL uses the plain {@code http://}
   * scheme a warning icon is prepended to make the insecure nature immediately
   * visible.
   *
   * @param url         target URL (used for href and tooltip)
   * @param displayText text rendered inside the anchor; falls back to {@code url}
   *                    when {@code null}
   * @return a single {@link Anchor} for HTTPS/other URLs; a
   *         {@link HorizontalLayout} (icon + anchor) for HTTP URLs
   */
  public static Component httpAwareLink(String url, String displayText) {
    if (url == null || url.isBlank()) {
      return new Anchor("", displayText != null ? displayText : "");
    }
    final String text = displayText != null ? displayText : url;
    final var a = new Anchor(url, text);
    a.setTarget("_blank");
    a.getElement().setProperty("title", url);

    if (url.startsWith("http://")) {
      final var warn = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
      warn.getElement().getStyle().set("color", "var(--lumo-error-color)");
      warn.getElement().getStyle().set("font-size", "var(--lumo-icon-size-s)");
      warn.getElement().getStyle().set("flex-shrink", "0");
      warn.getElement().setProperty("title", "⚠ Insecure HTTP – data is not encrypted");
      final var wrap = new HorizontalLayout(warn, a);
      wrap.setSpacing(false);
      wrap.getStyle().set("gap", "4px");
      wrap.setAlignItems(Alignment.CENTER);
      return wrap;
    }
    return a;
  }
}