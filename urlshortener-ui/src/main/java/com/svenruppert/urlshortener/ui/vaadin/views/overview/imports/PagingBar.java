package com.svenruppert.urlshortener.ui.vaadin.views.overview.imports;



import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;

@CssImport("./styles/paging-bar.css")
public final class PagingBar
    extends HorizontalLayout
    implements I18nSupport {

  private static final String C_ROOT = "paging-bar";
  private static final String C_BUTTON = "paging-bar__button";
  private static final String C_INFO = "paging-bar__info";

  private static final String K_PREV = "overview.paging.prev";
  private static final String K_NEXT = "overview.paging.next";
  private static final String K_INFO = "overview.paging.info";

  private final int size;

  private final Button prev = new Button();
  private final Button next = new Button();
  private final Span info = new Span();

  private int page = 1;
  private int total = 0;
  private Runnable onPageChange = () -> { };

  public PagingBar(int size) {
    this.size = size;

    addClassName(C_ROOT);
    setDefaultVerticalComponentAlignment(CENTER);

    prev.setText(tr(K_PREV, "‹ Prev"));
    next.setText(tr(K_NEXT, "Next ›"));

    prev.addClassName(C_BUTTON);
    next.addClassName(C_BUTTON);
    info.addClassName(C_INFO);

    add(prev, info, next);
    updateInfo();

    prev.addClickListener(_ -> {
      if (page > 1) {
        page--;
        updateInfo();
        onPageChange.run();
      }
    });

    next.addClickListener(_ -> {
      if (page < maxPages()) {
        page++;
        updateInfo();
        onPageChange.run();
      }
    });
  }

  public void setOnPageChange(Runnable r) {
    this.onPageChange = r != null ? r : () -> { };
  }

  public int page() {
    return page;
  }

  public int size() {
    return size;
  }

  public void setPage(int page) {
    this.page = Math.max(1, page);
    updateInfo();
  }

  public void setTotal(int total) {
    this.total = Math.max(0, total);
    updateInfo();
  }

  private int maxPages() {
    if (total <= 0) return 1;
    return (int) Math.ceil(total / (double) size);
  }

  private void updateInfo() {
    info.setText(tr(
        K_INFO,
        "Page {0} / {1} (total {2})",
        page,
        maxPages(),
        total
    ));

    prev.setEnabled(page > 1);
    next.setEnabled(page < maxPages());
  }
}