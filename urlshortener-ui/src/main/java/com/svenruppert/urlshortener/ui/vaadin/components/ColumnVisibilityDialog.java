package com.svenruppert.urlshortener.ui.vaadin.components;



import com.svenruppert.urlshortener.ui.vaadin.tools.ColumnVisibilityService;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;

import java.util.Map;
import java.util.Objects;

@CssImport("./styles/column-visibility-dialog.css")
public final class ColumnVisibilityDialog<T> extends Dialog implements I18nSupport {

  private static final String CLASS_ROOT = "column-visibility-dialog";
  private static final String CLASS_FORM = "column-visibility-dialog__form";
  private static final String CLASS_CB   = "column-visibility-dialog__checkbox";

  // i18n keys (leading element: overview)
  private static final String NS = "overview.columns";
  private static final String KEY_TITLE = NS + ".title";
  private static final String KEY_COL_PREFIX = NS + ".column.";
  private static final String KEY_CLOSE = "common.close";

  public ColumnVisibilityDialog(Grid<T> grid, ColumnVisibilityService service) {
    Objects.requireNonNull(grid, "grid must not be null");
    Objects.requireNonNull(service, "service must not be null");

    addClassName(CLASS_ROOT);

    setHeaderTitle(tr(KEY_TITLE, "Columns"));
    setModality(ModalityMode.STRICT);
    setDraggable(true);
    setResizable(true);

    var form = new FormLayout();
    form.addClassName(CLASS_FORM);
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("480px", 2),
        new FormLayout.ResponsiveStep("900px", 3)
    );

    var knownKeys = grid.getColumns().stream()
        .map(Grid.Column::getKey)
        .filter(Objects::nonNull)
        .toList();

    Map<String, Boolean> state = service.mergeWithDefaults(knownKeys);

    grid.getColumns().forEach(col -> {
      final String key = col.getKey();
      if (key == null) return;

      boolean visible = state.getOrDefault(key, true);
      col.setVisible(visible);

      String fallbackLabel = (col.getHeaderText() != null && !col.getHeaderText().isBlank())
          ? col.getHeaderText()
          : key;

      String label = tr(KEY_COL_PREFIX + key, fallbackLabel);

      var cb = new Checkbox(label, visible);
      cb.addClassName(CLASS_CB);

      cb.addValueChangeListener(ev -> {
        boolean v = Boolean.TRUE.equals(ev.getValue());
        col.setVisible(v);
        service.setSingle(key, v);
      });

      form.add(cb);
    });

    var btnClose = new Button(tr(KEY_CLOSE, "Close"), _ -> close());
    btnClose.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getFooter().add(btnClose);

    add(form);
  }
}