package com.svenruppert.urlshortener.ui.vaadin.views;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.urlmapping.BulkShortenRequest;
import com.svenruppert.urlshortener.core.urlmapping.BulkShortenResponse;
import com.svenruppert.urlshortener.core.urlmapping.BulkShortenResponse.BulkShortenItemResult;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateResponse;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateResponse.ExistingShortlinkInfo;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateResponse.ValidationItemResult;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateResponse.ValidationStatus;
import com.svenruppert.urlshortener.ui.vaadin.MainLayout;
import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.VisibleFor;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.tools.UiLinks;
import com.svenruppert.urlshortener.ui.vaadin.tools.UrlShortenerClientFactory;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Two-phase Bulk-Create workflow:
 *
 * <ol>
 *   <li><b>Validate</b> – URLs from the textarea are validated server-side (no persistence).
 *       Results appear in the grid. The textarea is cleared so URLs are not processed twice.
 *       More URLs can be added and validated into the same work set at any time.</li>
 *   <li><b>Create</b> – Once the textarea is empty and every grid row is &ldquo;creatable&rdquo;
 *       (VALID or HAS_EXISTING), the primary button switches to &ldquo;Create Links&rdquo;.
 *       Clicking it sends all creatable rows to the bulk-create endpoint.</li>
 * </ol>
 *
 * <p>The grid is the primary work set. Users can edit individual URLs inline (triggering
 * automatic re-validation of the edited row) and remove rows at any time.
 */
@Route(value = BulkCreateView.PATH, layout = MainLayout.class)
@VisibleFor(AppRole.USER)
public class BulkCreateView extends VerticalLayout implements HasLogger, I18nSupport {

  public static final String PATH = "bulk-create";

  // ── i18n keys ──────────────────────────────────────────────────────────────

  private static final String K_TITLE              = "bulkCreate.title";
  private static final String K_LABEL_URLS          = "bulkCreate.field.urls";
  private static final String K_PH_URLS             = "bulkCreate.field.urls.placeholder";
  private static final String K_LABEL_EXPIRES_DATE  = "bulkCreate.field.expiresDate";
  private static final String K_LABEL_ACTIVE        = "bulkCreate.field.active";
  private static final String K_BTN_VALIDATE        = "bulkCreate.btn.validate";
  private static final String K_BTN_CREATE          = "bulkCreate.btn.create";
  private static final String K_BTN_RESET           = "common.reset";
  private static final String K_BTN_COPY_ALL        = "bulkCreate.btn.copyAll";
  private static final String K_BTN_RETRY_FAILED    = "bulkCreate.btn.retryFailed";
  private static final String K_COL_INDEX           = "bulkCreate.col.index";
  private static final String K_COL_URL             = "bulkCreate.col.url";
  private static final String K_COL_SHORT_URL       = "bulkCreate.col.shortUrl";
  private static final String K_COL_STATUS          = "bulkCreate.col.status";
  private static final String K_COL_EXISTING        = "bulkCreate.col.existing";
  private static final String K_COL_ERROR           = "bulkCreate.col.error";
  private static final String K_COL_ACTIONS         = "bulkCreate.col.actions";
  private static final String K_PREVIEW             = "bulkCreate.preview";
  private static final String K_SUMMARY             = "bulkCreate.summary";
  private static final String K_TOAST_EMPTY         = "bulkCreate.toast.empty";
  private static final String K_TOAST_DONE          = "bulkCreate.toast.done";
  private static final String K_TOAST_COPIED        = "bulkCreate.toast.copied";
  private static final String K_TOAST_LIMIT         = "bulkCreate.toast.limit";
  private static final String K_TOAST_NO_FAILED     = "bulkCreate.toast.noFailed";
  private static final String K_TOAST_REMOVED       = "bulkCreate.toast.removed";
  private static final String K_TOAST_VALIDATE_DONE = "bulkCreate.toast.validateDone";
  private static final String K_TOAST_NOTHING       = "bulkCreate.toast.nothingToValidate";
  private static final String K_TOAST_REVALIDATED   = "bulkCreate.toast.revalidated";
  private static final String K_ACTION_EDIT_TT      = "bulkCreate.action.editTooltip";
  private static final String K_ACTION_REMOVE_TT    = "bulkCreate.action.removeTooltip";
  private static final String K_HINT_HAS_EXISTING   = "bulkCreate.hint.hasExisting";
  private static final String K_STATE_VALID         = "bulkCreate.state.valid";
  private static final String K_STATE_HAS_EXISTING  = "bulkCreate.state.hasExisting";
  private static final String K_STATE_INVALID_URL   = "bulkCreate.state.invalidUrl";
  private static final String K_STATE_TOO_LONG      = "bulkCreate.state.tooLong";
  private static final String K_STATE_DUP_BATCH     = "bulkCreate.state.duplicateBatch";
  private static final String K_STATE_DUP_GRID      = "bulkCreate.state.duplicateGrid";
  private static final String K_STATE_CREATED          = "bulkCreate.state.created";
  private static final String K_STATE_FAILED           = "bulkCreate.state.createFailed";
  private static final String K_STATE_CONFIRMED        = "bulkCreate.state.confirmedExisting";
  private static final String K_DIALOG_EXISTING_TITLE  = "bulkCreate.dialog.existingTitle";
  private static final String K_DIALOG_EXISTING_CLOSE  = "bulkCreate.dialog.existingClose";
  private static final String K_ACTION_CONFIRM_ANYWAY  = "bulkCreate.action.confirmAnyway";
  private static final String K_TOOLTIP_HTTP_UNSAFE   = "common.url.insecureHttp";
  private static final String K_HINT_VALIDATE_FIRST   = "bulkCreate.hint.validateFirst";
  private static final String K_HINT_PROTOCOL_VARIANT = "bulkCreate.hint.protocolVariant";

  // ── Components ─────────────────────────────────────────────────────────────

  private final URLShortenerClient client = UrlShortenerClientFactory.newInstance();
  private static final ZoneId ZONE = ZoneId.systemDefault();

  private final TextArea       urlsArea        = new TextArea();
  private final DateTimePicker expiresDateTime = new DateTimePicker();
  private final Checkbox       cbActive        = new Checkbox(true);
  private final Span       previewLabel      = new Span();
  private final Button     primaryButton     = new Button();
  private final Button     resetButton       = new Button();
  private final Button     copyAllButton     = new Button();
  private final Button     retryFailedButton = new Button();
  private final Span       summaryLabel      = new Span();
  private final Grid<WorkItem> resultsGrid   = new Grid<>();

  /** Ordered work set — single source of truth for the grid. */
  private final List<WorkItem> workItems = new ArrayList<>();

  // ── Constructor ────────────────────────────────────────────────────────────

  public BulkCreateView() {
    setSizeFull();
    setSpacing(true);
    setPadding(true);

    add(new H2(tr(K_TITLE, "Bulk Create Short Links")));

    // TextArea — input buffer only; cleared after each Validate action
    urlsArea.setLabel(tr(K_LABEL_URLS, "Target URLs (one per line)"));
    urlsArea.setPlaceholder(tr(K_PH_URLS, "https://example.com\nhttps://other.org"));
    urlsArea.setWidthFull();
    urlsArea.setMinHeight("140px");
    urlsArea.addValueChangeListener(_ -> updatePreview());

    // Expiry options
    expiresDateTime.setLabel(tr(K_LABEL_EXPIRES_DATE, "Default expiry date (optional)"));
    expiresDateTime.setStep(Duration.ofMinutes(30));
    expiresDateTime.setWeekNumbersVisible(true);
    cbActive.setLabel(tr(K_LABEL_ACTIVE, "Active"));

    // Preview line
    previewLabel.getStyle()
        .set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");

    // Primary button — label and style change dynamically
    primaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    primaryButton.addClickListener(_ -> onPrimaryButtonClick());

    resetButton.setText(tr(K_BTN_RESET, "Reset"));
    resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    resetButton.addClickListener(_ -> reset());

    copyAllButton.setText(tr(K_BTN_COPY_ALL, "Copy all short URLs"));
    copyAllButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    copyAllButton.setVisible(false);
    copyAllButton.addClickListener(_ -> copyAllShortUrls());

    retryFailedButton.setText(tr(K_BTN_RETRY_FAILED, "Retry failed"));
    retryFailedButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    retryFailedButton.setVisible(false);
    retryFailedButton.addClickListener(_ -> retryFailed());

    var optionsRow = new HorizontalLayout(expiresDateTime, cbActive);
    optionsRow.setAlignItems(Alignment.END);

    var actionRow = new HorizontalLayout(
        primaryButton, resetButton, copyAllButton, retryFailedButton);
    actionRow.setSpacing(true);

    configureGrid();

    add(urlsArea, previewLabel, optionsRow, actionRow, summaryLabel, resultsGrid);
    resultsGrid.setVisible(false);
    summaryLabel.setVisible(false);

    updatePrimaryButton();
  }

  // ── Grid ───────────────────────────────────────────────────────────────────

  private void configureGrid() {
    // # – display position
    resultsGrid.addColumn(item -> String.valueOf(workItems.indexOf(item) + 1))
        .setHeader(tr(K_COL_INDEX, "#"))
        .setFlexGrow(0)
        .setWidth("55px");

    // URL column — supports inline editor; HTTP links get a warning icon
    final var urlCol = resultsGrid.addComponentColumn(item -> {
      final String url = item.getUrl();
      if (url != null && url.startsWith("http://")) {
        return UiLinks.httpAwareLink(url, url);
      }
      return new com.vaadin.flow.component.html.Span(url != null ? url : "");
    })
        .setHeader(tr(K_COL_URL, "Original URL"))
        .setFlexGrow(3)
        .setResizable(true);

    // Short URL column — shown after creation
    resultsGrid.addComponentColumn(item -> {
      if (item.getCreatedShortUrl() != null) {
        final var link = new Anchor(item.getCreatedShortUrl(), item.getCreatedShortUrl());
        link.setTarget("_blank");
        return link;
      }
      return new Span();
    })
        .setHeader(tr(K_COL_SHORT_URL, "Short URL"))
        .setFlexGrow(2)
        .setResizable(true);

    // Status badge
    resultsGrid.addComponentColumn(this::statusBadge)
        .setHeader(tr(K_COL_STATUS, "Status"))
        .setFlexGrow(0)
        .setWidth("165px");

    // Existing shortlinks — clickable button that opens a detail + confirm dialog
    resultsGrid.addComponentColumn(item -> {
      final var codes = item.getExistingShortCodes();
      if (item.getState() == WorkItemState.HAS_EXISTING
          && codes != null
          && !codes.isEmpty()) {
        final var btn = new Button(
            tr(K_HINT_HAS_EXISTING, "{0} shortlink(s)", codes.size()));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.addClickListener(_ -> showExistingShortlinksDialog(item, codes));
        return btn;
      }
      return new Span();
    })
        .setHeader(tr(K_COL_EXISTING, "Existing Links"))
        .setFlexGrow(1)
        .setResizable(true);

    // Error / info
    resultsGrid.addColumn(item -> item.getMessage() != null ? item.getMessage() : "")
        .setHeader(tr(K_COL_ERROR, "Error"))
        .setFlexGrow(2)
        .setResizable(true);

    // Actions
    resultsGrid.addComponentColumn(this::buildActionButtons)
        .setHeader(tr(K_COL_ACTIONS, ""))
        .setFlexGrow(0)
        .setWidth("90px");

    // ── Inline grid editor ────────────────────────────────────────────────────
    final var binder = new Binder<WorkItem>();
    final var editor = resultsGrid.getEditor();
    editor.setBinder(binder);

    final var urlEditorField = new TextField();
    urlEditorField.setWidthFull();
    binder.forField(urlEditorField).bind(WorkItem::getUrl, WorkItem::setUrl);
    urlCol.setEditorComponent(urlEditorField);

    // Re-validate the edited row when the editor closes
    editor.addCloseListener(event -> {
      final WorkItem edited = event.getItem();
      if (edited != null && !edited.getUrl().isBlank()) {
        revalidateItem(edited);
      }
    });

    resultsGrid.setWidthFull();
    resultsGrid.setHeight("420px");
  }

  private Span statusBadge(WorkItem item) {
    final String label;
    final String theme;
    switch (item.getState()) {
      case VALID -> {
        label = "\u2713 " + tr(K_STATE_VALID, "Valid");
        theme = "badge success";
      }
      case HAS_EXISTING -> {
        if (item.isConfirmed()) {
          label = "\u2713 " + tr(K_STATE_CONFIRMED, "Confirmed");
          theme = "badge success";
        } else {
          label = "\u26a0 " + tr(K_STATE_HAS_EXISTING, "Has Existing");
          theme = "badge contrast";
        }
      }
      case INVALID_URL -> {
        label = "\u2717 " + tr(K_STATE_INVALID_URL, "Invalid URL");
        theme = "badge error";
      }
      case TOO_LONG -> {
        label = "\u2717 " + tr(K_STATE_TOO_LONG, "Too Long");
        theme = "badge error";
      }
      case DUPLICATE_BATCH -> {
        label = "\u2717 " + tr(K_STATE_DUP_BATCH, "Duplicate (batch)");
        theme = "badge error";
      }
      case DUPLICATE_GRID -> {
        label = "\u2717 " + tr(K_STATE_DUP_GRID, "Duplicate (grid)");
        theme = "badge error";
      }
      case CREATED -> {
        label = "\u2713 " + tr(K_STATE_CREATED, "Created");
        theme = "badge success";
      }
      case CREATE_FAILED -> {
        label = "\u2717 " + tr(K_STATE_FAILED, "Failed");
        theme = "badge error";
      }
      default -> {
        label = "\u2013";
        theme = "badge";
      }
    }
    final var badge = new Span(label);
    badge.getElement().getThemeList().add(theme);
    return badge;
  }

  private HorizontalLayout buildActionButtons(WorkItem item) {
    final var row = new HorizontalLayout();
    row.setSpacing(false);
    row.getStyle().set("gap", "4px");

    // Edit button — same icon as Overview row actions
    final var editBtn = new Button(new Icon(VaadinIcon.EDIT));
    editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
    editBtn.getElement().setProperty("title",
        tr(K_ACTION_EDIT_TT, "Correct URL inline"));
    editBtn.addClickListener(_ -> {
      final var editor = resultsGrid.getEditor();
      if (editor.isOpen()) {
        editor.cancel();
      }
      editor.editItem(item);
    });

    // Delete button — same icon as Overview row actions
    final var removeBtn = new Button(new Icon(VaadinIcon.TRASH));
    removeBtn.addThemeVariants(
        ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    removeBtn.getElement().setProperty("title",
        tr(K_ACTION_REMOVE_TT, "Remove entry"));
    removeBtn.addClickListener(_ -> removeItem(item));

    row.add(editBtn, removeBtn);
    return row;
  }

  // ── Primary button state machine ───────────────────────────────────────────

  /**
   * Updates the primary button label, style, and enabled state.
   *
   * <ul>
   *   <li><b>Validate</b> (PRIMARY) – textarea has content <em>or</em> grid has blocking rows.</li>
   *   <li><b>Create</b>   (SUCCESS, grayed) – no blocking rows, but unconfirmed HAS_EXISTING items
   *       remain. The button is shown but disabled until the user confirms via the grid dialog.</li>
   *   <li><b>Create</b>   (SUCCESS, enabled) – textarea empty, no blocking, no unconfirmed
   *       warnings, at least one item ready to create.</li>
   * </ul>
   */
  private void updatePrimaryButton() {
    final boolean textAreaHasContent = !parseInputLines().isEmpty();
    final boolean hasBlocking     = workItems.stream().anyMatch(w -> w.getState().isBlocking());
    final boolean hasUnconfirmed  = workItems.stream()
        .anyMatch(w -> w.getState() == WorkItemState.HAS_EXISTING && !w.isConfirmed());
    final boolean hasReadyToCreate = workItems.stream().anyMatch(this::isReadyToCreate);

    final boolean workSetEmpty = workItems.isEmpty();

    if (!textAreaHasContent && !hasBlocking && !workSetEmpty) {
      // Work set has items, no blocking errors, textarea cleared → switch to Create mode
      primaryButton.setText(tr(K_BTN_CREATE, "Create Links"));
      primaryButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
      primaryButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
      // Enabled only when all HAS_EXISTING items are confirmed AND at least one is ready
      primaryButton.setEnabled(!hasUnconfirmed && hasReadyToCreate);
    } else {
      // Default: Validate mode (also covers initial empty state)
      primaryButton.setText(tr(K_BTN_VALIDATE, "Validate"));
      primaryButton.removeThemeVariants(ButtonVariant.LUMO_SUCCESS);
      primaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      // Enabled when there is something to validate (textarea content or blocking grid rows)
      primaryButton.setEnabled(textAreaHasContent || hasBlocking);
    }
  }

  private void onPrimaryButtonClick() {
    final boolean textAreaHasContent = !parseInputLines().isEmpty();
    final boolean hasBlocking = workItems.stream().anyMatch(w -> w.getState().isBlocking());

    if (!textAreaHasContent && !hasBlocking) {
      doCreate();
    } else {
      doValidate();
    }
  }

  // ── isReadyToCreate helper ─────────────────────────────────────────────────

  /**
   * A work item is ready to create if it is VALID, or if it is HAS_EXISTING and
   * the user has explicitly confirmed the warning via the existing-links dialog.
   */
  private boolean isReadyToCreate(WorkItem w) {
    return w.getState() == WorkItemState.VALID
        || (w.getState() == WorkItemState.HAS_EXISTING && w.isConfirmed());
  }

  // ── Validate ───────────────────────────────────────────────────────────────

  private void doValidate() {
    final List<String> newUrls = parseInputLines();

    // Blocking grid rows are included in the re-validation run
    final List<WorkItem> blockingItems = workItems.stream()
        .filter(w -> w.getState().isBlocking())
        .toList();

    if (newUrls.isEmpty() && blockingItems.isEmpty()) {
      notify(tr(K_TOAST_NOTHING, "Nothing to validate"), false);
      return;
    }

    // Build the list to validate: new textarea entries first, then blocking grid items
    final List<String> urlsToValidate = new ArrayList<>(newUrls);
    for (final var item : blockingItems) {
      if (!item.getUrl().isBlank() && !urlsToValidate.contains(item.getUrl())) {
        urlsToValidate.add(item.getUrl());
      }
    }

    if (urlsToValidate.size() > BulkShortenRequest.MAX_URLS) {
      notify(tr(K_TOAST_LIMIT,
          "Only the first {0} URLs will be processed (limit)",
          BulkShortenRequest.MAX_URLS), false);
      urlsToValidate.subList(BulkShortenRequest.MAX_URLS, urlsToValidate.size()).clear();
    }

    // Items not being re-validated represent the full established work set for
    // DUPLICATE_IN_GRID detection — includes VALID, HAS_EXISTING, CREATED, CREATE_FAILED
    final java.util.Set<WorkItem> toRevalidateSet = new java.util.HashSet<>(blockingItems);
    final List<String> existingUrls = workItems.stream()
        .filter(w -> !toRevalidateSet.contains(w) && !w.getUrl().isBlank())
        .map(WorkItem::getUrl)
        .toList();

    try {
      final BulkValidateResponse response = client.bulkValidate(urlsToValidate, existingUrls);

      // Replace old blocking rows with re-validation results
      workItems.removeAll(blockingItems);
      for (final ValidationItemResult r : response.getResults()) {
        workItems.add(WorkItem.fromValidation(r));
      }

      // Clear the textarea — URLs have been transferred to the work set
      urlsArea.setValue("");

      final long creatable = response.getResults().stream()
          .filter(ValidationItemResult::isCreatable).count();
      final long blocking  = response.getResults().stream()
          .filter(ValidationItemResult::isBlocking).count();

      refreshGrid();
      notify(tr(K_TOAST_VALIDATE_DONE,
          "Validated: {0} creatable, {1} blocking", creatable, blocking),
          blocking == 0);

    } catch (Exception e) {
      logger().error("Bulk validate failed", e);
      notify("Error: " + e.getMessage(), false);
    }
  }

  // ── Create ─────────────────────────────────────────────────────────────────

  private void doCreate() {
    final List<WorkItem> creatableItems = workItems.stream()
        .filter(this::isReadyToCreate)
        .toList();

    if (creatableItems.isEmpty()) {
      notify(tr(K_TOAST_EMPTY, "No creatable URLs in work set"), false);
      return;
    }

    final List<String> urls = creatableItems.stream()
        .map(WorkItem::getUrl)
        .toList();

    try {
      final BulkShortenResponse response = client.bulkShorten(
          urls, computeExpiresAt().orElse(null), cbActive.getValue());

      // Build lookup: originalUrl → result
      final Map<String, BulkShortenItemResult> byUrl = new HashMap<>();
      for (final var r : response.getResults()) {
        byUrl.put(r.getOriginalUrl(), r);
      }

      for (final var item : creatableItems) {
        final var r = byUrl.get(item.getUrl());
        if (r != null) {
          if (r.isSuccess()) {
            item.setState(WorkItemState.CREATED);
            item.setCreatedShortUrl(r.getShortUrl());
            item.setCreatedShortCode(r.getShortCode());
            item.setMessage(null);
          } else {
            item.setState(WorkItemState.CREATE_FAILED);
            item.setMessage(r.getErrorMessage());
          }
        }
      }

      refreshGrid();
      notify(tr(K_TOAST_DONE, "Done: {0} created, {1} failed",
          response.getSucceeded(), response.getFailed()),
          response.getFailed() == 0);

    } catch (Exception e) {
      logger().error("Bulk create failed", e);
      notify("Error: " + e.getMessage(), false);
    }
  }

  // ── Re-validate single item ────────────────────────────────────────────────

  /**
   * Re-validates a single grid item after inline editing.
   *
   * <p>All other work-set items (regardless of their current state) are passed as
   * {@code existingUrls} so that duplicate-in-grid detection is consistent with a
   * full Validate run. The item's confirmation flag is reset because a URL change
   * invalidates any previously given confirmation.
   */
  private void revalidateItem(WorkItem item) {
    if (item.getUrl().isBlank()) {
      return;
    }
    // Pass the complete remaining work set (all states) for consistent duplicate detection
    final List<String> existingUrls = workItems.stream()
        .filter(w -> w != item && !w.getUrl().isBlank())
        .map(WorkItem::getUrl)
        .toList();

    try {
      final BulkValidateResponse response =
          client.bulkValidate(List.of(item.getUrl()), existingUrls);
      final ValidationItemResult r = response.getResults().get(0);
      item.setState(WorkItem.mapValidationStatus(r.getStatus()));
      item.setExistingShortCodes(r.getExistingShortCodes());
      item.setExistingShortlinkInfos(r.getExistingShortlinkInfos());
      item.setMessage(r.getErrorMessage());
      item.setConfirmed(false); // URL changed → previous confirmation no longer applies
      refreshGrid();
      notify(tr(K_TOAST_REVALIDATED, "URL re-validated"), r.isCreatable());
    } catch (Exception e) {
      logger().error("Re-validation of single item failed", e);
      notify("Error: " + e.getMessage(), false);
    }
  }

  // ── Existing shortlinks detail + confirm dialog ────────────────────────────

  /**
   * Opens a dialog listing the shortcodes that already point to the item's URL.
   * The dialog also provides an explicit "Confirm — create anyway" action so the
   * user can consciously proceed despite the warning.
   */
  private void showExistingShortlinksDialog(WorkItem item, List<String> shortCodes) {
    final var dialog = new Dialog();
    dialog.setHeaderTitle(tr(K_DIALOG_EXISTING_TITLE, "Existing Shortlinks"));

    final var content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);
    content.setWidth("420px");

    final List<ExistingShortlinkInfo> infos = item.getExistingShortlinkInfos();

    if (!infos.isEmpty()) {
      // Enriched display: show active/expiry status + protocol variant warning
      for (final ExistingShortlinkInfo info : infos) {
        final String fullUrl =
            com.svenruppert.urlshortener.core.DefaultValues.SHORTCODE_BASE_URL + info.getShortCode();

        final var linkRow = new HorizontalLayout();
        linkRow.setSpacing(false);
        linkRow.getStyle().set("gap", "8px");
        linkRow.setAlignItems(Alignment.CENTER);

        // Link (HTTP-aware)
        final var linkComp = UiLinks.httpAwareLink(fullUrl, info.getShortCode());
        linkRow.add(linkComp);

        // Active badge
        final var activeBadge = new Span(info.isActive() ? "\u2713 Active" : "\u2717 Inactive");
        activeBadge.getElement().getThemeList().add("badge pill small");
        activeBadge.getElement().getThemeList().add(info.isActive() ? "success" : "error");
        linkRow.add(activeBadge);

        // Expiry badge
        final var expiryStatus = com.svenruppert.urlshortener.ui.vaadin.components.ExpiryBadgeFactory
            .computeStatusText(java.util.Optional.ofNullable(info.getExpiresAt()));
        final var expiryBadge = new Span(expiryStatus.text());
        expiryBadge.getElement().getThemeList().add("badge pill small");
        expiryBadge.getElement().getThemeList().add(expiryStatus.theme());
        linkRow.add(expiryBadge);

        // Protocol variant warning
        if (info.isProtocolVariant()) {
          final var variantBadge = new Span("\u26a0 Protocol variant");
          variantBadge.getElement().getThemeList().add("badge pill small error");
          variantBadge.getElement().setProperty("title",
              "This shortlink targets the http/https counterpart of your URL");
          linkRow.add(variantBadge);
        }

        content.add(linkRow);
      }
    } else {
      // Fallback: just show links (no enriched info available)
      for (final String code : shortCodes) {
        final String fullUrl =
            com.svenruppert.urlshortener.core.DefaultValues.SHORTCODE_BASE_URL + code;
        final var link = new Anchor(fullUrl, fullUrl);
        link.setTarget("_blank");
        content.add(link);
      }
    }

    dialog.add(content);

    final var closeBtn = new Button(
        tr(K_DIALOG_EXISTING_CLOSE, "Close"), _ -> dialog.close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    final var confirmBtn = new Button(
        tr(K_ACTION_CONFIRM_ANYWAY, "Confirm \u2013 create anyway"));
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    confirmBtn.addClickListener(_ -> {
      item.setConfirmed(true);
      dialog.close();
      refreshGrid();
    });

    dialog.getFooter().add(closeBtn, confirmBtn);
    dialog.open();
  }

  // ── Remove item ────────────────────────────────────────────────────────────

  private void removeItem(WorkItem item) {
    workItems.remove(item);
    refreshGrid();
    notify(tr(K_TOAST_REMOVED, "Entry removed"), false);
  }

  // ── Retry CREATE_FAILED ────────────────────────────────────────────────────

  private void retryFailed() {
    final List<WorkItem> failedItems = workItems.stream()
        .filter(w -> w.getState() == WorkItemState.CREATE_FAILED)
        .toList();

    if (failedItems.isEmpty()) {
      notify(tr(K_TOAST_NO_FAILED, "No failed entries to retry"), false);
      return;
    }

    final List<String> urls = failedItems.stream().map(WorkItem::getUrl).toList();

    try {
      final BulkShortenResponse response = client.bulkShorten(
          urls, computeExpiresAt().orElse(null), cbActive.getValue());

      final Map<String, BulkShortenItemResult> byUrl = new HashMap<>();
      for (final var r : response.getResults()) {
        byUrl.put(r.getOriginalUrl(), r);
      }

      for (final var item : failedItems) {
        final var r = byUrl.get(item.getUrl());
        if (r != null && r.isSuccess()) {
          item.setState(WorkItemState.CREATED);
          item.setCreatedShortUrl(r.getShortUrl());
          item.setCreatedShortCode(r.getShortCode());
          item.setMessage(null);
        }
      }

      refreshGrid();
      notify(tr(K_TOAST_DONE, "Done: {0} created, {1} failed",
          response.getSucceeded(), response.getFailed()),
          response.getFailed() == 0);

    } catch (Exception e) {
      logger().error("Retry failed", e);
      notify("Error: " + e.getMessage(), false);
    }
  }

  // ── Copy all short URLs ────────────────────────────────────────────────────

  private void copyAllShortUrls() {
    final String text = workItems.stream()
        .filter(w -> w.getCreatedShortUrl() != null)
        .map(WorkItem::getCreatedShortUrl)
        .collect(Collectors.joining("\n"));

    if (text.isBlank()) {
      return;
    }
    UI.getCurrent().getPage().executeJs(
        "navigator.clipboard.writeText($0).catch(()=>{})", text);
    notify(tr(K_TOAST_COPIED, "Short URLs copied to clipboard"), true);
  }

  // ── Reset ──────────────────────────────────────────────────────────────────

  private void reset() {
    urlsArea.clear();
    expiresDateTime.clear();
    cbActive.setValue(true);
    previewLabel.setText("");
    workItems.clear();
    resultsGrid.setItems(List.of());
    resultsGrid.setVisible(false);
    summaryLabel.setVisible(false);
    copyAllButton.setVisible(false);
    retryFailedButton.setVisible(false);
    updatePrimaryButton();
  }

  // ── Refresh grid + status ──────────────────────────────────────────────────

  private void refreshGrid() {
    resultsGrid.setItems(new ArrayList<>(workItems));
    resultsGrid.setVisible(!workItems.isEmpty());

    final long created  = workItems.stream()
        .filter(w -> w.getState() == WorkItemState.CREATED).count();
    final long failed   = workItems.stream()
        .filter(w -> w.getState() == WorkItemState.CREATE_FAILED).count();
    final long blocking = workItems.stream()
        .filter(w -> w.getState().isBlocking()).count();

    summaryLabel.setText(tr(K_SUMMARY,
        "Total: {0} | Created: {1} | Failed: {2}",
        workItems.size(), created, failed + blocking));
    summaryLabel.setVisible(!workItems.isEmpty());

    copyAllButton.setVisible(created > 0);
    retryFailedButton.setVisible(failed > 0);
    updatePrimaryButton();
  }

  // ── Preview (textarea count only) ─────────────────────────────────────────

  private void updatePreview() {
    final List<String> lines = parseInputLines();
    previewLabel.setText(lines.isEmpty()
        ? ""
        : tr(K_PREVIEW, "{0} URL(s) entered", lines.size()));
    updatePrimaryButton();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private List<String> parseInputLines() {
    final String raw = urlsArea.getValue();
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split("\\R"))
        .map(String::strip)
        .filter(s -> !s.isBlank())
        .toList();
  }

  private Optional<Instant> computeExpiresAt() {
    final LocalDateTime ldt = expiresDateTime.getValue();
    if (ldt == null) {
      return Optional.empty();
    }
    return Optional.of(ldt.atZone(ZONE).toInstant());
  }

  private void notify(String message, boolean success) {
    final var n = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
    n.addThemeVariants(success
        ? NotificationVariant.LUMO_SUCCESS
        : NotificationVariant.LUMO_PRIMARY);
  }

  // ── WorkItemState enum ─────────────────────────────────────────────────────

  enum WorkItemState {

    /** Passes all validation checks; no existing shortlinks found. */
    VALID,

    /** Passes validation but existing shortlinks already target this URL (non-blocking warning). */
    HAS_EXISTING,

    /** Format validation failed (includes EMPTY input). Blocking. */
    INVALID_URL,

    /** URL exceeds maximum length. Blocking. */
    TOO_LONG,

    /** Duplicate within the submitted batch. Blocking. */
    DUPLICATE_BATCH,

    /** Already present in the work set. Blocking. */
    DUPLICATE_GRID,

    /** Successfully created in the Create phase. */
    CREATED,

    /** Creation attempt failed. */
    CREATE_FAILED;

    /** {@code true} for states that must be resolved before creation can proceed. */
    boolean isBlocking() {
      return this == INVALID_URL
          || this == TOO_LONG
          || this == DUPLICATE_BATCH
          || this == DUPLICATE_GRID;
    }

    /** {@code true} for states that allow the Create action to proceed. */
    boolean isCreatable() {
      return this == VALID || this == HAS_EXISTING;
    }
  }

  // ── WorkItem ───────────────────────────────────────────────────────────────

  static final class WorkItem {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @SuppressWarnings("unused")
    private final int id = SEQ.incrementAndGet();

    private String url;
    private WorkItemState state;
    private List<String> existingShortCodes = List.of();
    private List<ExistingShortlinkInfo> existingShortlinkInfos = List.of();
    private String message;
    private String createdShortUrl;
    private String createdShortCode;
    /**
     * {@code true} when the user has explicitly confirmed a HAS_EXISTING warning
     * via the existing-links dialog. Resets to {@code false} on re-validation.
     */
    private boolean confirmed = false;

    WorkItem() {
    }

    /** Creates a WorkItem from a server-side validation result. */
    static WorkItem fromValidation(ValidationItemResult r) {
      final var item = new WorkItem();
      final String normalized = r.getNormalizedUrl();
      item.url = (normalized != null && !normalized.isBlank())
          ? normalized
          : r.getOriginalUrl();
      item.state = mapValidationStatus(r.getStatus());
      item.existingShortCodes = r.getExistingShortCodes() != null
          ? r.getExistingShortCodes()
          : List.of();
      item.existingShortlinkInfos = r.getExistingShortlinkInfos() != null
          ? r.getExistingShortlinkInfos()
          : List.of();
      item.message = r.getErrorMessage();
      return item;
    }

    /** Maps a server-side {@link ValidationStatus} to the UI-local {@link WorkItemState}. */
    static WorkItemState mapValidationStatus(ValidationStatus s) {
      if (s == null) {
        return WorkItemState.INVALID_URL;
      }
      return switch (s) {
        case VALID                   -> WorkItemState.VALID;
        case HAS_EXISTING_SHORTLINKS -> WorkItemState.HAS_EXISTING;
        case EMPTY, INVALID_URL      -> WorkItemState.INVALID_URL;
        case TOO_LONG                -> WorkItemState.TOO_LONG;
        case DUPLICATE_IN_BATCH      -> WorkItemState.DUPLICATE_BATCH;
        case DUPLICATE_IN_GRID       -> WorkItemState.DUPLICATE_GRID;
      };
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getUrl() {
      return url != null ? url : "";
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public WorkItemState getState() {
      return state;
    }

    public void setState(WorkItemState state) {
      this.state = state;
    }

    public List<String> getExistingShortCodes() {
      return existingShortCodes;
    }

    public void setExistingShortCodes(List<String> existingShortCodes) {
      this.existingShortCodes = existingShortCodes != null ? existingShortCodes : List.of();
    }

    public List<ExistingShortlinkInfo> getExistingShortlinkInfos() {
      return existingShortlinkInfos;
    }

    public void setExistingShortlinkInfos(List<ExistingShortlinkInfo> existingShortlinkInfos) {
      this.existingShortlinkInfos = existingShortlinkInfos != null
          ? existingShortlinkInfos
          : List.of();
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getCreatedShortUrl() {
      return createdShortUrl;
    }

    public void setCreatedShortUrl(String createdShortUrl) {
      this.createdShortUrl = createdShortUrl;
    }

    public String getCreatedShortCode() {
      return createdShortCode;
    }

    public void setCreatedShortCode(String createdShortCode) {
      this.createdShortCode = createdShortCode;
    }

    public boolean isConfirmed() {
      return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
      this.confirmed = confirmed;
    }
  }
}