package com.svenruppert.urlshortener.ui.vaadin.views.overview;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;

@CssImport("./styles/bulk-actions-bar.css")
public class BulkActionsBar
    extends Composite<HorizontalLayout>
    implements HasLogger, I18nSupport {

  private static final String C_ROOT = "bulk-actions-bar";
  private static final String C_INFO = "bulk-actions-bar__info";
  private static final String C_BTN  = "bulk-actions-bar__btn";
  private static final String C_ICON = "bulk-actions-bar__icon";

  // i18n keys (Overview-leading namespace)
  private static final String K_TT_DELETE = "overview.bulk.tooltip.delete";
  private static final String K_TT_SET_EXPIRY = "overview.bulk.tooltip.setExpiry";
  private static final String K_TT_CLEAR_EXPIRY = "overview.bulk.tooltip.clearExpiry";
  private static final String K_TT_ACTIVATE = "overview.bulk.tooltip.activate";
  private static final String K_TT_DEACTIVATE = "overview.bulk.tooltip.deactivate";

  private static final String K_DLG_BULK_DELETE_TITLE = "overview.bulk.delete.title";
  private static final String K_DLG_BULK_DELETE_EXAMPLES = "overview.bulk.delete.examples";
  private static final String K_DLG_BULK_DELETE_FALLBACK = "overview.bulk.delete.fallback";

  private static final String K_DLG_SET_EXPIRY_TITLE = "overview.bulk.expiry.set.title";
  private static final String K_DLG_SET_EXPIRY_DATE = "overview.bulk.expiry.set.date";
  private static final String K_DLG_SET_EXPIRY_APPLY = "overview.bulk.expiry.set.apply";

  private static final String K_DLG_CLEAR_EXPIRY_TITLE = "overview.bulk.expiry.clear.title";
  private static final String K_DLG_CLEAR_EXPIRY_BODY = "overview.bulk.expiry.clear.body";
  private static final String K_DLG_CLEAR_EXPIRY_CONFIRM = "overview.bulk.expiry.clear.confirm";

  private static final String K_DLG_SET_ACTIVE_TITLE = "overview.bulk.active.title";
  private static final String K_DLG_SET_ACTIVE_BODY = "overview.bulk.active.body";
  private static final String K_DLG_SET_ACTIVE_CONFIRM = "overview.bulk.active.confirm";

  private static final String K_WORD_ACTIVATE = "overview.bulk.word.activate";
  private static final String K_WORD_DEACTIVATE = "overview.bulk.word.deactivate";
  private static final String K_WORD_ACTIVE = "overview.bulk.word.active";
  private static final String K_WORD_INACTIVE = "overview.bulk.word.inactive";

  private static final String K_COMMON_DELETE = "common.delete";
  private static final String K_COMMON_CANCEL = "common.cancel";

  private final URLShortenerClient urlShortenerClient;
  private final Grid<ShortUrlMapping> grid;
  private final OverviewView holdingComponent;

  private final Button bulkDeleteBtn = new Button();
  private final Button bulkSetExpiryBtn = new Button();
  private final Button bulkClearExpiryBtn = new Button();
  private final Button bulkActivateBtn = new Button();
  private final Button bulkDeactivateBtn = new Button();

  private final Span selectionInfo = new Span();

  public BulkActionsBar(URLShortenerClient urlShortenerClient,
                        Grid<ShortUrlMapping> grid,
                        OverviewView holdingComponent) {
    this.urlShortenerClient = urlShortenerClient;
    this.grid = grid;
    this.holdingComponent = holdingComponent;

    buildBulkBar();
    addListeners();
  }

  private HorizontalLayout bulkBar() {
    return this.getContent();
  }

  private void buildBulkBar() {
    bulkBar().addClassName(C_ROOT);

    selectionInfo.addClassName(C_INFO);

    setupIconButton(
        bulkDeleteBtn,
        VaadinIcon.TRASH,
        tr(K_TT_DELETE, "Delete selected links"),
        "danger"
    );

    setupIconButton(
        bulkSetExpiryBtn,
        VaadinIcon.CALENDAR_CLOCK,
        tr(K_TT_SET_EXPIRY, "Set expiry for selected"),
        "primary"
    );

    setupIconButton(
        bulkClearExpiryBtn,
        VaadinIcon.CALENDAR_CLOCK,
        tr(K_TT_CLEAR_EXPIRY, "Clear expiry for selected"),
        "neutral"
    );

    setupIconButton(
        bulkActivateBtn,
        VaadinIcon.CHECK_CIRCLE,
        tr(K_TT_ACTIVATE, "Activate selected"),
        "success"
    );

    setupIconButton(
        bulkDeactivateBtn,
        VaadinIcon.CLOSE_CIRCLE,
        tr(K_TT_DEACTIVATE, "Deactivate selected"),
        "danger"
    );

    bulkBar().removeAll();
    bulkBar().add(
        selectionInfo,
        bulkDeleteBtn,
        bulkSetExpiryBtn,
        bulkClearExpiryBtn,
        bulkActivateBtn,
        bulkDeactivateBtn
    );

    bulkBar().setWidthFull();
    bulkBar().setSpacing(true);
    bulkBar().setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    bulkBar().setVisible(false);
  }

  private void setupIconButton(Button btn,
                               VaadinIcon icon,
                               String tooltip,
                               String role) {

    btn.addClassName(C_BTN);
    btn.addClassName(C_BTN + "--" + role);

    Icon ic = icon.create();
    ic.addClassName(C_ICON);

    btn.setIcon(ic);
    btn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
    btn.getElement().setProperty("title", tooltip);
  }

  private void addListeners() {
    bulkDeleteBtn.addClickListener(_ -> confirmBulkDeleteSelected());
    bulkSetExpiryBtn.addClickListener(_ -> openBulkSetExpiryDialog());
    bulkClearExpiryBtn.addClickListener(_ -> confirmBulkClearExpirySelected());
    bulkActivateBtn.addClickListener(_ -> confirmBulkActivateSelected());
    bulkDeactivateBtn.addClickListener(_ -> confirmBulkDeactivateSelected());
  }

  public void confirmBulkDeleteSelected() {
    var selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      Notifications.noSelection();
      return;
    }

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr(
        K_DLG_BULK_DELETE_TITLE,
        "Delete {0} short links?",
        selected.size()
    ));

    var exampleCodes = selected.stream()
        .map(ShortUrlMapping::shortCode)
        .sorted()
        .limit(5)
        .toList();

    if (!exampleCodes.isEmpty()) {
      String preview = String.join(", ", exampleCodes);
      if (selected.size() > 5) preview += ", …";
      dialog.add(new Text(tr(
          K_DLG_BULK_DELETE_EXAMPLES,
          "Examples: {0}",
          preview
      )));
    } else {
      dialog.add(new Text(tr(
          K_DLG_BULK_DELETE_FALLBACK,
          "Delete selected short links?"
      )));
    }

    Button confirm = new Button(tr(K_COMMON_DELETE, "Delete"), _ -> {
      int success = 0;
      int failed = 0;

      for (var m : selected) {
        try {
          boolean ok = urlShortenerClient.delete(m.shortCode());
          if (ok) success++; else failed++;
        } catch (IOException ex) {
          logger().error("Bulk delete failed for {}", m.shortCode(), ex);
          failed++;
        }
      }

      dialog.close();
      grid.deselectAll();
      holdingComponent.safeRefresh();
      Notifications.deletedAndNotDeleted(success, failed);
    });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, LUMO_ERROR);

    Button cancel = new Button(tr(K_COMMON_CANCEL, "Cancel"), _ -> dialog.close());
    dialog.getFooter().add(new HorizontalLayout(confirm, cancel));
    dialog.open();
  }

  private void openBulkSetExpiryDialog() {
    var selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      Notifications.noSelection();
      return;
    }

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr(
        K_DLG_SET_EXPIRY_TITLE,
        "Set expiry for {0} short links",
        selected.size()
    ));

    final var expiresField = new DateTimePicker(tr(K_DLG_SET_EXPIRY_DATE, "Date"));
    expiresField.setStep(Duration.ofMinutes(30));
    expiresField.setWeekNumbersVisible(true);

    dialog.add(expiresField);

    Button cancel = new Button(tr(K_COMMON_CANCEL, "Cancel"), _ -> dialog.close());
    Button apply = new Button(tr(K_DLG_SET_EXPIRY_APPLY, "Apply"), _ -> {
      if (expiresField.getValue() == null) {
        Notifications.noDateSelected();
        return;
      }

      final var zdt = expiresField.getValue().atZone(ZoneId.systemDefault());
      Instant expiresAt = zdt.toInstant();

      int success = 0;
      int failed = 0;

      for (ShortUrlMapping m : selected) {
        try {
          boolean ok = urlShortenerClient.edit(
              m.shortCode(),
              m.originalUrl(),
              expiresAt,
              m.active()
          );
          if (ok) success++; else failed++;
        } catch (IOException ex) {
          logger().error("Bulk set expiry failed for {}", m.shortCode(), ex);
          failed++;
        }
      }

      dialog.close();
      grid.deselectAll();
      holdingComponent.safeRefresh();
      Notifications.updatedAndNotUpdated(success, failed);
    });
    apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(new HorizontalLayout(cancel, apply));
    dialog.open();
  }

  private void confirmBulkClearExpirySelected() {
    var selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      Notifications.noSelection();
      return;
    }

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr(
        K_DLG_CLEAR_EXPIRY_TITLE,
        "Remove expiry for {0} short links?",
        selected.size()
    ));

    dialog.add(new Text(tr(
        K_DLG_CLEAR_EXPIRY_BODY,
        "This will remove the expiry date from all selected short links. They will no longer expire automatically."
    )));

    Button cancel = new Button(tr(K_COMMON_CANCEL, "Cancel"), _ -> dialog.close());
    Button confirm = new Button(tr(
        K_DLG_CLEAR_EXPIRY_CONFIRM,
        "Remove expiry"
    ), _ -> {
      dialog.close();
      bulkClearExpiry(selected);
    });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(new HorizontalLayout(cancel, confirm));
    dialog.open();
  }

  private void bulkClearExpiry(Set<ShortUrlMapping> selected) {
    int success = 0;
    int failed = 0;

    for (var m : selected) {
      try {
        boolean ok = urlShortenerClient.edit(
            m.shortCode(),
            m.originalUrl(),
            null,
            m.active()
        );
        if (ok) success++; else failed++;
      } catch (IOException ex) {
        logger().error("Bulk clear expiry failed for {}", m.shortCode(), ex);
        failed++;
      }
    }

    grid.deselectAll();
    holdingComponent.safeRefresh();
    Notifications.updatedAndNotUpdated(success, failed);
  }

  private void bulkSetActive(Set<ShortUrlMapping> selected, boolean activate) {
    int success = 0;
    int failed = 0;

    for (var m : selected) {
      try {
        var ok = urlShortenerClient.toggleActive(m.shortCode(), activate);
        if (ok) success++; else failed++;
      } catch (IOException ex) {
        logger().error("Toggle active state failed for {}", m.shortCode(), ex);
        failed++;
      }
    }

    grid.deselectAll();
    holdingComponent.safeRefresh();
    Notifications.updatedAndNotUpdated(success, failed);
  }

  private void confirmBulkSetActiveSelected(boolean activate) {
    var selected = grid.getSelectedItems();
    if (selected.isEmpty()) {
      Notifications.noSelection();
      return;
    }

    final String verb = activate
        ? tr(K_WORD_ACTIVATE, "activate")
        : tr(K_WORD_DEACTIVATE, "deactivate");

    final String stateAfter = activate
        ? tr(K_WORD_ACTIVE, "active")
        : tr(K_WORD_INACTIVE, "inactive");

    final String verbCap = activate
        ? capitalize(verb)
        : capitalize(verb);

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr(
        K_DLG_SET_ACTIVE_TITLE,
        "{0} all {1} short links?",
        verbCap,
        selected.size()
    ));

    dialog.add(new Text(tr(
        K_DLG_SET_ACTIVE_BODY,
        "This will {0} all selected short links. They will be {1} afterwards.",
        verb,
        stateAfter
    )));

    Button cancel = new Button(tr(K_COMMON_CANCEL, "Cancel"), _ -> dialog.close());
    Button confirm = new Button(tr(
        K_DLG_SET_ACTIVE_CONFIRM,
        "{0} All",
        verbCap
    ), _ -> {
      dialog.close();
      bulkSetActive(Set.copyOf(selected), activate);
    });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    dialog.getFooter().add(new HorizontalLayout(cancel, confirm));
    dialog.open();
  }

  private void confirmBulkActivateSelected() {
    confirmBulkSetActiveSelected(true);
  }

  private void confirmBulkDeactivateSelected() {
    confirmBulkSetActiveSelected(false);
  }

  public void setButtonsEnabled(boolean hasSelection) {
    bulkDeleteBtn.setEnabled(hasSelection);
    bulkSetExpiryBtn.setEnabled(hasSelection);
    bulkClearExpiryBtn.setEnabled(hasSelection);
    bulkActivateBtn.setEnabled(hasSelection);
    bulkDeactivateBtn.setEnabled(hasSelection);
  }

  public void selectionInfoText(String txt) {
    selectionInfo.setText(txt);
    selectionInfo.setVisible(!txt.isBlank());
  }

  private static String capitalize(String s) {
    if (s == null || s.isBlank()) return s;
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}