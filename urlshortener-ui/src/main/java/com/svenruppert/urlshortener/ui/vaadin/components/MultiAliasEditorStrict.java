package com.svenruppert.urlshortener.ui.vaadin.components;



import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CssImport("./styles/multi-alias-editor-strict.css")
public class MultiAliasEditorStrict
    extends VerticalLayout
    implements I18nSupport {

  private static final String RX = "^[A-Za-z0-9_-]{3,64}$";

  // CSS classes
  private static final String C_ROOT = "multi-alias-editor";
  private static final String C_BULK = "multi-alias-editor__bulk";
  private static final String C_TOOLBAR = "multi-alias-editor__toolbar";
  private static final String C_GRID = "multi-alias-editor__grid";
  private static final String C_STATUS = "multi-alias-editor__status";

  // i18n keys
  private static final String NS = "multiAlias";

  private static final String K_BULK_LABEL = NS + ".bulk.label";
  private static final String K_BULK_PLACEHOLDER = NS + ".bulk.placeholder";

  private static final String K_BTN_TAKE_OVER = NS + ".btn.takeOver";
  private static final String K_BTN_VALIDATE_ALL = NS + ".btn.validateAll";

  private static final String K_COL_ALIAS = NS + ".col.alias";
  private static final String K_COL_PREVIEW = NS + ".col.preview";
  private static final String K_COL_STATUS = NS + ".col.status";

  private static final String K_ACTION_REMOVE = NS + ".action.remove";
  private static final String K_BTN_REMOVE = NS + ".btn.remove";

  private static final String K_TOAST_NO_ALIASES = NS + ".toast.noAliases";
  private static final String K_TOAST_INSERTED = NS + ".toast.inserted"; // Inserted: {0}

  private static final String K_STATUS_NEW = NS + ".status.new";
  private static final String K_STATUS_VALID = NS + ".status.valid";
  private static final String K_STATUS_FORMAT = NS + ".status.format";
  private static final String K_STATUS_TAKEN = NS + ".status.taken";
  private static final String K_STATUS_ERROR = NS + ".status.error";
  private static final String K_STATUS_SAVED = NS + ".status.saved";

  private static final String K_MSG_SAVED = NS + ".msg.saved";
  private static final String K_MSG_ERROR = NS + ".msg.error";
  private static final String K_MSG_FORMAT = NS + ".msg.format";
  private static final String K_MSG_DUPLICATE = NS + ".msg.duplicate";
  private static final String K_MSG_ALIAS_TAKEN = NS + ".msg.aliasTaken";
  private static final String K_MSG_CHECK_FAILED = NS + ".msg.checkFailed";

  private final Grid<Row> grid = new Grid<>(Row.class, false);
  private final TextArea bulk = new TextArea();
  private final Button insertBtn = new Button();
  private final Button validateBtn = new Button();

  private final String baseUrl;
  private final Function<String, Boolean> isAliasFree;

  public MultiAliasEditorStrict(String baseUrl, Function<String, Boolean> isAliasFree) {
    this.baseUrl = baseUrl;
    this.isAliasFree = isAliasFree;
    build();
  }

  public void validateAll() {
    var items = new ArrayList<>(grid.getListDataView().getItems().toList());
    items.forEach(this::validateRow);
    grid.getDataProvider().refreshAll();
  }

  public List<String> getValidAliases() {
    return grid.getListDataView()
        .getItems()
        .filter(r -> r.getStatus() == Status.VALID)
        .map(Row::getAlias)
        .collect(Collectors.toList());
  }

  public void markSaved(String alias) {
    setStatus(alias, Status.SAVED, tr(K_MSG_SAVED, "saved"));
  }

  public void markError(String alias, String message) {
    setStatus(alias, Status.ERROR, (message == null ? tr(K_MSG_ERROR, "error") : message));
  }

  public long countOpen() {
    return grid.getListDataView()
        .getItems()
        .filter(r -> r.getStatus() != Status.SAVED)
        .count();
  }

  public void clearAllRows() {
    grid.setItems(new ArrayList<>());
  }

  private void setStatus(String alias, Status s, String msg) {
    grid.getListDataView().getItems().forEach(r -> {
      if (Objects.equals(r.getAlias(), alias)) {
        r.setStatus(s);
        r.setMsg(msg);
      }
    });
    grid.getDataProvider().refreshAll();
  }

  private void build() {
    addClassName(C_ROOT);

    setPadding(false);
    setSpacing(true);

    bulk.addClassName(C_BULK);
    bulk.setWidthFull();
    bulk.setValueChangeMode(ValueChangeMode.LAZY);
    bulk.setClearButtonVisible(true);
    bulk.setLabel(tr(K_BULK_LABEL, "Aliases (comma/space/newline)"));
    bulk.setPlaceholder(tr(K_BULK_PLACEHOLDER, "e.g.\nnews-2025\npromo_x\nabc123"));

    insertBtn.setText(tr(K_BTN_TAKE_OVER, "Take over"));
    validateBtn.setText(tr(K_BTN_VALIDATE_ALL, "Validate all"));

    insertBtn.addClickListener(_ -> parseBulk());
    validateBtn.addClickListener(_ -> validateAll());

    var toolbar = new HorizontalLayout(insertBtn, validateBtn);
    toolbar.addClassName(C_TOOLBAR);
    toolbar.setSpacing(true);

    configureGrid();

    add(bulk, toolbar, grid);
  }

  private void configureGrid() {
    grid.addClassName(C_GRID);

    grid.addComponentColumn(row -> {
      var tf = new TextField();
      tf.setWidthFull();
      tf.setMaxLength(64);
      tf.setPattern(RX);
      tf.setValue(Objects.requireNonNullElse(row.getAlias(), ""));
      tf.setEnabled(row.getStatus() != Status.SAVED);
      tf.addValueChangeListener(ev -> {
        row.setAlias(ev.getValue());
        validateRow(row);
        grid.getDataProvider().refreshItem(row);
      });
      return tf;
    }).setHeader(tr(K_COL_ALIAS, "Alias")).setFlexGrow(1);

    grid.addColumn(r -> baseUrl + Objects.requireNonNullElse(r.getAlias(), ""))
        .setHeader(tr(K_COL_PREVIEW, "Preview"))
        .setAutoWidth(true);

    grid.addComponentColumn(row -> {
      var lbl = switch (row.getStatus()) {
        case NEW -> tr(K_STATUS_NEW, "New");
        case VALID -> tr(K_STATUS_VALID, "Valid");
        case INVALID_FORMAT -> tr(K_STATUS_FORMAT, "Format");
        case CONFLICT -> tr(K_STATUS_TAKEN, "Taken");
        case ERROR -> tr(K_STATUS_ERROR, "Error");
        case SAVED -> tr(K_STATUS_SAVED, "Saved");
      };

      var badge = new Span(lbl);
      badge.addClassName(C_STATUS);

      // Status für CSS selektierbar machen
      badge.getElement().setAttribute("data-status", row.getStatus().name().toLowerCase());

      if (row.getMsg() != null && !row.getMsg().isBlank()) {
        badge.setTitle(row.getMsg());
      }
      return badge;
    }).setHeader(tr(K_COL_STATUS, "Status")).setAutoWidth(true);

    grid.addComponentColumn(row -> {
      var del = new Button(tr(K_BTN_REMOVE, "✕"), e -> {
        var items = new ArrayList<>(grid.getListDataView().getItems().toList());
        items.remove(row);
        grid.setItems(items);
      });
      del.getElement().setProperty("title", tr(K_ACTION_REMOVE, "Remove"));
      return del;
    }).setHeader("").setAutoWidth(true);

    grid.setItems(new ArrayList<>());
    grid.setAllRowsVisible(false);
  }

  private void parseBulk() {
    var text = Objects.requireNonNullElse(bulk.getValue(), "");
    var tokens = Arrays.stream(text.split("[,;\\s]+"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();

    if (tokens.isEmpty()) {
      Notifications.warningKey(K_TOAST_NO_ALIASES, "No aliases to insert");
      return;
    }

    Set<String> existing = grid.getListDataView().getItems()
        .map(Row::getAlias)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

    var view = grid.getListDataView();
    int added = 0;
    for (String tok : tokens) {
      if (existing.contains(tok)) continue;
      var r = new Row(tok);
      validateRow(r);
      view.addItem(r);
      existing.add(tok);
      added++;
    }

    grid.getDataProvider().refreshAll();
    bulk.clear();
    Notifications.infoKey(K_TOAST_INSERTED, "Inserted: {0}", added);
  }

  private void validateRow(Row r) {
    var a = Objects.requireNonNullElse(r.getAlias(), "");

    if (!a.matches(RX)) {
      r.setStatus(Status.INVALID_FORMAT);
      r.setMsg(tr(K_MSG_FORMAT, "3–64: A–Z a–z 0–9 - _"));
      return;
    }

    long same = grid.getListDataView().getItems()
        .filter(x -> x != r && Objects.equals(a, x.getAlias()))
        .count();
    if (same > 0) {
      r.setStatus(Status.CONFLICT);
      r.setMsg(tr(K_MSG_DUPLICATE, "Duplicate in list"));
      return;
    }

    if (isAliasFree != null) {
      try {
        if (!isAliasFree.apply(a)) {
          r.setStatus(Status.CONFLICT);
          r.setMsg(tr(K_MSG_ALIAS_TAKEN, "Alias taken"));
          return;
        }
      } catch (Exception ex) {
        r.setStatus(Status.ERROR);
        r.setMsg(tr(K_MSG_CHECK_FAILED, "Check failed"));
        return;
      }
    }

    r.setStatus(Status.VALID);
    r.setMsg("");
  }

  public enum Status { NEW, VALID, INVALID_FORMAT, CONFLICT, ERROR, SAVED }

  public static final class Row {
    private String alias;
    private Status status = Status.NEW;
    private String msg = "";

    Row(String a) {
      this.alias = a;
    }

    public String getAlias() {
      return alias;
    }

    public void setAlias(String a) {
      this.alias = a;
    }

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status s) {
      this.status = s;
    }

    public String getMsg() {
      return msg;
    }

    public void setMsg(String m) {
      this.msg = m;
    }
  }
}