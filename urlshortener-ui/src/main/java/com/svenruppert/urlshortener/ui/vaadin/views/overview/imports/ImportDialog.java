package com.svenruppert.urlshortener.ui.vaadin.views.overview.imports;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.urlmapping.imports.ImportResult;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.svenruppert.urlshortener.core.DefaultValues.APPLICATION_ZIP;
import static com.svenruppert.urlshortener.core.DefaultValues.IMPORT_MAX_ZIP_BYTES;
import static com.svenruppert.urlshortener.core.urlmapping.imports.JsonFieldExtractor.extractJsonInt;
import static com.svenruppert.urlshortener.core.urlmapping.imports.JsonFieldExtractor.extractJsonString;

public final class ImportDialog
    extends Dialog
    implements HasLogger, I18nSupport {

  private final URLShortenerClient client;

  private final Upload upload = new Upload();
  private byte[] zipBytes;

  private final Button btnValidate = new Button();
  private final Button btnApply = new Button();
  private final Button btnClose = new Button();

  private final Span lblStaging = new Span("-");
  private final Span lblNew = new Span("0");
  private final Span lblConflicts = new Span("0");
  private final Span lblInvalid = new Span("0");

  private final Tab tabConflicts = new Tab();
  private final Tab tabInvalid = new Tab();
  private final Tabs tabs = new Tabs(tabConflicts, tabInvalid);

  private final Grid<ConflictRow> gridConflicts = new Grid<>(ConflictRow.class, false);
  private final Grid<InvalidRow> gridInvalid = new Grid<>(InvalidRow.class, false);

  private final PagingBar pagingConflicts = new PagingBar(50);
  private final PagingBar pagingInvalid = new PagingBar(50);

  private final Runnable onImportApplied;
  private final Checkbox chkSkipConflicts = new Checkbox();
  private final Div applyHint = new Div();

  private String stagingId;

  // i18n keys
  private static final String K_TITLE = "overview.import.title";

  private static final String K_SECTION_UPLOAD = "overview.import.section.uploadZip";
  private static final String K_SECTION_PREVIEW = "overview.import.section.preview";

  private static final String K_BTN_VALIDATE = "overview.import.btn.validate";
  private static final String K_BTN_APPLY = "overview.import.btn.apply";
  private static final String K_BTN_CLOSE = "overview.import.btn.close";

  private static final String K_SKIP_CONFLICTS = "overview.import.checkbox.skipConflicts";

  private static final String K_HINT_INITIAL = "overview.import.hint.initial";
  private static final String K_HINT_NEED_VALIDATE = "overview.import.hint.needValidate";
  private static final String K_HINT_DISABLED_INVALID = "overview.import.hint.disabledInvalid";
  private static final String K_HINT_DISABLED_CONFLICTS = "overview.import.hint.disabledConflicts";
  private static final String K_HINT_READY = "overview.import.hint.ready";
  private static final String K_HINT_APPLIED = "overview.import.hint.applied";

  private static final String K_APPLY_SKIP_CONFLICTS = "overview.import.apply.skipConflicts";

  private static final String K_TAB_CONFLICTS = "overview.import.tab.conflicts";
  private static final String K_TAB_INVALID = "overview.import.tab.invalid";

  private static final String K_SUMMARY_STAGING = "overview.import.summary.stagingId";
  private static final String K_SUMMARY_NEW = "overview.import.summary.new";
  private static final String K_SUMMARY_CONFLICTS = "overview.import.summary.conflicts";
  private static final String K_SUMMARY_INVALID = "overview.import.summary.invalid";

  private static final String K_GRID_C_SHORTCODE = "overview.import.grid.conflicts.shortCode";
  private static final String K_GRID_C_DIFF = "overview.import.grid.conflicts.diff";
  private static final String K_GRID_C_EXISTING_URL = "overview.import.grid.conflicts.existingUrl";
  private static final String K_GRID_C_INCOMING_URL = "overview.import.grid.conflicts.incomingUrl";
  private static final String K_GRID_C_EXISTING_ACTIVE = "overview.import.grid.conflicts.existingActive";
  private static final String K_GRID_C_INCOMING_ACTIVE = "overview.import.grid.conflicts.incomingActive";
  private static final String K_GRID_C_EXISTING_EXPIRES = "overview.import.grid.conflicts.existingExpiresAt";
  private static final String K_GRID_C_INCOMING_EXPIRES = "overview.import.grid.conflicts.incomingExpiresAt";

  private static final String K_GRID_I_SHORTCODE = "overview.import.grid.invalid.shortCode";
  private static final String K_GRID_I_REASON = "overview.import.grid.invalid.reason";

  private static final String K_TOAST_NO_ZIP = "overview.import.toast.noZip";
  private static final String K_TOAST_VALIDATED = "overview.import.toast.validated";
  private static final String K_TOAST_APPLIED = "overview.import.toast.applied";

  public ImportDialog(URLShortenerClient client, Runnable onImportApplied) {
    this.client = client;
    this.onImportApplied = onImportApplied != null ? onImportApplied : () -> { };

    // Dialog config
    setHeaderTitle(tr(K_TITLE, "Import"));
    setModality(ModalityMode.STRICT);
    setResizable(true);
    setDraggable(true);
    setWidth("1100px");
    setHeight("750px");

    // Tabs
    tabConflicts.setLabel(tr(K_TAB_CONFLICTS, "Conflicts"));
    tabInvalid.setLabel(tr(K_TAB_INVALID, "Invalid"));
    tabs.setSelectedTab(tabConflicts);

    // Checkbox + hint
    chkSkipConflicts.setLabel(tr(K_SKIP_CONFLICTS, "Skip conflicts on apply"));
    chkSkipConflicts.setEnabled(false);
    chkSkipConflicts.setValue(false);
    chkSkipConflicts.addValueChangeListener(_ -> updateApplyState());

    applyHint.addClassNames("importdialog-applyhint");
    applyHint.setText(tr(K_HINT_INITIAL, "Upload a ZIP and validate to continue."));

    // Upload
    upload.setAcceptedFileTypes(".zip", APPLICATION_ZIP);
    upload.setMaxFiles(1);
    upload.setMaxFileSize(IMPORT_MAX_ZIP_BYTES);

    upload.addAllFinishedListener(_ -> {
      logger().info("Upload finished..");
      btnApply.setEnabled(false);
    });

    upload.addFileRejectedListener(event -> {
      String errorMessage = event.getErrorMessage();
      Notification notification = Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
      notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
      logger().warn("upload.addFileRejectedListener - {}", errorMessage);
    });

    UploadHandler inMemoryUploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
      logger().info("uploaded file: fileName: {} , contentLength {}", metadata.fileName(), metadata.contentLength());
      zipBytes = bytes;
      btnValidate.setEnabled(true);
    });
    upload.setUploadHandler(inMemoryUploadHandler);

    // Buttons
    btnValidate.setText(tr(K_BTN_VALIDATE, "Validate"));
    btnValidate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    btnValidate.setEnabled(false);
    btnValidate.addClickListener(_ -> validate());

    btnApply.setText(tr(K_BTN_APPLY, "Apply Import"));
    btnApply.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    btnApply.setEnabled(false);
    btnApply.addClickListener(_ -> applyImport());

    btnClose.setText(tr(K_BTN_CLOSE, "Close"));
    btnClose.addClickListener(_ -> close());

    // Grids + content
    initGrids();
    add(buildContent());
    getFooter().add(btnClose, btnValidate, btnApply);
  }

  private static Component labelPair(String label, Span value) {
    var l = new Span(label + ": ");
    l.getStyle().set("font-weight", "600");
    var row = new HorizontalLayout(l, value);
    row.setSpacing(false);
    row.getStyle().set("gap", "6px");
    return row;
  }

  private static int parseInt(String s, int def) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return def;
    }
  }

  private Component buildContent() {
    var summary = new HorizontalLayout(
        labelPair(tr(K_SUMMARY_STAGING, "stagingId"), lblStaging),
        labelPair(tr(K_SUMMARY_NEW, "new"), lblNew),
        labelPair(tr(K_SUMMARY_CONFLICTS, "conflicts"), lblConflicts),
        labelPair(tr(K_SUMMARY_INVALID, "invalid"), lblInvalid)
    );
    summary.setWidthFull();

    var applyRow = new HorizontalLayout(chkSkipConflicts, applyHint);
    applyRow.setWidthFull();
    applyRow.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    applyRow.expand(applyHint);

    var tabContent = new VerticalLayout();
    tabContent.setPadding(false);
    tabContent.setSpacing(false);
    tabContent.setSizeFull();

    tabs.addSelectedChangeListener(_ -> renderTab(tabContent));

    var root = new VerticalLayout(
        new H3(tr(K_SECTION_UPLOAD, "Upload ZIP")),
        upload,
        new H3(tr(K_SECTION_PREVIEW, "Preview")),
        summary,
        applyRow,
        tabs,
        tabContent
    );

    root.setSizeFull();
    root.setPadding(false);

    renderTab(tabContent);
    return root;
  }

  private void initGrids() {
    gridConflicts.addColumn(ConflictRow::shortCode).setHeader(tr(K_GRID_C_SHORTCODE, "shortCode")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.addColumn(ConflictRow::diff).setHeader(tr(K_GRID_C_DIFF, "diff")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.addColumn(ConflictRow::existingUrl).setHeader(tr(K_GRID_C_EXISTING_URL, "existingUrl")).setAutoWidth(true);
    gridConflicts.addColumn(ConflictRow::incomingUrl).setHeader(tr(K_GRID_C_INCOMING_URL, "incomingUrl")).setAutoWidth(true);
    gridConflicts.addColumn(ConflictRow::existingActive).setHeader(tr(K_GRID_C_EXISTING_ACTIVE, "existingActive")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.addColumn(ConflictRow::incomingActive).setHeader(tr(K_GRID_C_INCOMING_ACTIVE, "incomingActive")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.addColumn(ConflictRow::existingExpiresAt).setHeader(tr(K_GRID_C_EXISTING_EXPIRES, "existingExpiresAt")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.addColumn(ConflictRow::incomingExpiresAt).setHeader(tr(K_GRID_C_INCOMING_EXPIRES, "incomingExpiresAt")).setAutoWidth(true).setFlexGrow(0);
    gridConflicts.setSizeFull();

    gridInvalid.addColumn(InvalidRow::shortCode).setHeader(tr(K_GRID_I_SHORTCODE, "shortCode")).setAutoWidth(true).setFlexGrow(0);
    gridInvalid.addColumn(InvalidRow::reason).setHeader(tr(K_GRID_I_REASON, "reason")).setAutoWidth(true);
    gridInvalid.setSizeFull();
  }

  private void renderTab(VerticalLayout container) {
    container.removeAll();
    container.setSizeFull();

    if (tabs.getSelectedTab() == tabInvalid) {
      container.add(pagingInvalid, gridInvalid);
      container.expand(gridInvalid);
    } else {
      container.add(pagingConflicts, gridConflicts);
      container.expand(gridConflicts);
    }
  }

  private void validate() {
    try {
      if (zipBytes == null || zipBytes.length == 0) {
        Notification.show(tr(K_TOAST_NO_ZIP, "No ZIP uploaded."), 2500, Notification.Position.TOP_CENTER);
        return;
      }

      String previewJson = client.importValidateRaw(zipBytes);
      logger().info("importValidateRaw response ->|{}|<-", previewJson);

      this.stagingId = extractJsonString(previewJson, "stagingId");
      int newItems = extractJsonInt(previewJson, "newItems", 0);
      int conflicts = extractJsonInt(previewJson, "conflicts", 0);
      int invalid = extractJsonInt(previewJson, "invalid", 0);

      lblStaging.setText(stagingId == null ? "-" : stagingId);
      lblNew.setText(String.valueOf(newItems));
      lblConflicts.setText(String.valueOf(conflicts));
      lblInvalid.setText(String.valueOf(invalid));

      List<ConflictRow> conflictRows = new ArrayList<>();
      try (StringReader r = new StringReader(previewJson)) {
        for (String obj : new ItemsArrayIterator(r, "conflictItems")) {
          Map<String, String> m = JsonUtils.parseJson(obj);
          conflictRows.add(ConflictRow.from(m));
        }
      }

      List<InvalidRow> invalidRows = new ArrayList<>();
      try (StringReader r = new StringReader(previewJson)) {
        for (String obj : new ItemsArrayIterator(r, "invalidItems")) {
          Map<String, String> m = JsonUtils.parseJson(obj);
          invalidRows.add(InvalidRow.from(m));
        }
      }

      gridConflicts.setItems(conflictRows);
      gridInvalid.setItems(invalidRows);

      pagingConflicts.setTotal(conflictRows.size());
      pagingConflicts.setPage(1);
      pagingInvalid.setTotal(invalidRows.size());
      pagingInvalid.setPage(1);

      chkSkipConflicts.setEnabled(conflicts > 0);
      chkSkipConflicts.setValue(false);

      updateApplyState();

      Notification.show(tr(K_TOAST_VALIDATED, "Import validated."), 2500, Notification.Position.TOP_CENTER);

    } catch (Exception ex) {
      Notifications.operationFailed(ex);
      logger().warn("validate {}", ex.getMessage());
    }
  }

  private void updateApplyState() {
    btnApply.setEnabled(false);
    btnApply.setText(tr(K_BTN_APPLY, "Apply Import"));

    if (stagingId == null || stagingId.isBlank()) {
      applyHint.setText(tr(K_HINT_NEED_VALIDATE, "Validate an import archive first."));
      return;
    }

    int invalid = parseInt(lblInvalid.getText(), 0);
    int conflicts = parseInt(lblConflicts.getText(), 0);

    if (invalid > 0) {
      applyHint.setText(tr(
          K_HINT_DISABLED_INVALID,
          "Apply disabled: {0} invalid item(s). Fix/remove them before applying.",
          invalid
      ));
      btnApply.setText(tr(K_BTN_APPLY, "Apply Import"));
      return;
    }

    if (conflicts > 0 && chkSkipConflicts.getValue()) {
      btnApply.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    } else {
      btnApply.removeThemeVariants(ButtonVariant.LUMO_CONTRAST);
    }

    if (conflicts > 0 && !chkSkipConflicts.getValue()) {
      applyHint.setText(tr(
          K_HINT_DISABLED_CONFLICTS,
          "Apply disabled: {0} conflict(s). Tick \u201cSkip conflicts on apply\u201d to proceed.",
          conflicts
      ));
      btnApply.setText(tr(K_BTN_APPLY, "Apply Import"));
      return;
    }

    btnApply.setEnabled(true);
    btnApply.setText(conflicts > 0
                         ? tr(K_APPLY_SKIP_CONFLICTS, "Apply (skip conflicts)")
                         : tr(K_BTN_APPLY, "Apply Import"));
    applyHint.setText(tr(K_HINT_READY, "Ready to apply import."));
  }

  private void applyImport() {
    if (stagingId == null || stagingId.isBlank()) return;

    try {
      ImportResult res = client.importApply(stagingId);
      logger().info("importApply response ->|{}|<-", res);
      onImportApplied.run();
      close();

      Notification.show(tr(
          K_TOAST_APPLIED,
          "Import applied: created={0}, skippedConflicts={1}, invalid={2}",
          res.created(), res.skippedConflicts(), res.invalid()
      ), 3500, Notification.Position.TOP_CENTER);

      btnApply.setEnabled(false);
      btnValidate.setEnabled(false);
      chkSkipConflicts.setEnabled(false);
      chkSkipConflicts.setValue(false);
      applyHint.setText(tr(K_HINT_APPLIED, "Import applied. Upload a new ZIP to continue."));

    } catch (Exception ex) {
      Notifications.operationFailed(ex);
      logger().warn("applyImport {}", ex.getMessage());
    }
  }

  public record ConflictRow(
      String shortCode,
      String diff,
      String existingUrl,
      String incomingUrl,
      String existingActive,
      String incomingActive,
      String existingExpiresAt,
      String incomingExpiresAt
  ) {
    static ConflictRow from(Map<String, String> m) {
      return new ConflictRow(
          m.get("shortCode"),
          m.get("diff"),
          m.get("existingUrl"),
          m.get("incomingUrl"),
          m.get("existingActive"),
          m.get("incomingActive"),
          m.get("existingExpiresAt"),
          m.get("incomingExpiresAt")
      );
    }
  }

  public record InvalidRow(String shortCode, String reason) {
    static InvalidRow from(Map<String, String> m) {
      return new InvalidRow(m.get("shortCode"), m.get("reason"));
    }
  }
}