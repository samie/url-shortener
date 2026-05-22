package com.svenruppert.urlshortener.ui.vaadin.views.statistics;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.StatisticsClient;
import com.svenruppert.urlshortener.core.statistics.StatisticsImportResponse;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import static com.svenruppert.urlshortener.core.DefaultValues.APPLICATION_ZIP;
import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_IMPORT_MAX_ZIP_BYTES;

public final class StatisticsImportDialog
    extends Dialog
    implements HasLogger, I18nSupport {

  private static final String K_TITLE = "statistics.import.title";
  private static final String K_HINT = "statistics.import.hint";
  private static final String K_BTN_IMPORT = "statistics.import.btn.import";
  private static final String K_BTN_CLOSE = "statistics.import.btn.close";
  private static final String K_SUCCESS = "statistics.import.success";
  private static final String K_FAILED = "statistics.import.failed";

  private final StatisticsClient client;
  private final Runnable onImported;

  private final Upload upload = new Upload();
  private final Span hint = new Span();
  private final Button btnImport = new Button();
  private final Button btnClose = new Button();

  private byte[] zipBytes;

  public StatisticsImportDialog(StatisticsClient client, Runnable onImported) {
    this.client = client;
    this.onImported = onImported != null ? onImported : () -> { };

    setHeaderTitle(tr(K_TITLE, "Import statistics"));
    setModality(ModalityMode.STRICT);
    setDraggable(false);
    setResizable(false);
    setWidth("520px");

    hint.setText(tr(K_HINT, "Select a ZIP produced by the statistics export. Events are appended to existing data; aggregates are rebuilt automatically for the imported date range."));

    // Upload — limit by the int-safe portion of the configured cap to fit Vaadin's API.
    int maxBytes = (int) Math.min(STATISTICS_IMPORT_MAX_ZIP_BYTES, (long) Integer.MAX_VALUE);
    upload.setAcceptedFileTypes(".zip", APPLICATION_ZIP);
    upload.setMaxFiles(1);
    upload.setMaxFileSize(maxBytes);

    UploadHandler inMemoryHandler = UploadHandler.inMemory((metadata, bytes) -> {
      logger().info("Statistics import upload: fileName={} contentLength={}",
                    metadata.fileName(), metadata.contentLength());
      zipBytes = bytes;
      btnImport.setEnabled(true);
    });
    upload.setUploadHandler(inMemoryHandler);

    upload.addFileRejectedListener(event -> {
      Notification n = Notification.show(event.getErrorMessage(), 5000, Notification.Position.MIDDLE);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    });

    btnImport.setText(tr(K_BTN_IMPORT, "Import"));
    btnImport.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    btnImport.setEnabled(false);
    btnImport.addClickListener(_ -> runImport());

    btnClose.setText(tr(K_BTN_CLOSE, "Close"));
    btnClose.addClickListener(_ -> close());

    var root = new VerticalLayout(hint, upload);
    root.setPadding(false);
    root.setSpacing(true);
    add(root);
    getFooter().add(btnClose, btnImport);
  }

  private void runImport() {
    if (zipBytes == null || zipBytes.length == 0) {
      return;
    }
    btnImport.setEnabled(false);
    try {
      StatisticsImportResponse result = client.importZip(zipBytes);
      String msg = tr(K_SUCCESS,
                      "Imported {0} events ({1} skipped) — range {2} .. {3}",
                      result.importedEvents(),
                      result.skippedLines(),
                      String.valueOf(result.from()),
                      String.valueOf(result.to()));
      Notification.show(msg, 5000, Notification.Position.BOTTOM_START)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      onImported.run();
      close();
    } catch (Exception ex) {
      logger().warn("Statistics import failed", ex);
      Notification.show(tr(K_FAILED, "Import failed: {0}", ex.getMessage()),
                        7000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      btnImport.setEnabled(true);
    }
  }
}
