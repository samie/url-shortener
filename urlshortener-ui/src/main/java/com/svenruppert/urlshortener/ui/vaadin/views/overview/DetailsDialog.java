package com.svenruppert.urlshortener.ui.vaadin.views.overview;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.core.validation.UrlValidator;
import com.svenruppert.urlshortener.ui.vaadin.components.MultiAliasEditorStrict;
import com.svenruppert.urlshortener.ui.vaadin.events.MappingCreatedOrChanged;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.tools.UrlShortenerClientFactory;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.shared.Registration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static com.svenruppert.urlshortener.core.DefaultValues.SHORTCODE_BASE_URL;
import static com.svenruppert.urlshortener.ui.vaadin.components.ExpiryBadgeFactory.computeStatusText;
import static com.svenruppert.urlshortener.ui.vaadin.tools.UiActions.copyToClipboard;

@CssImport("./styles/details-dialog.css")
public class DetailsDialog
    extends Dialog
    implements HasLogger, I18nSupport {

  public static final ZoneId ZONE = ZoneId.systemDefault();
  private static final DateTimeFormatter DATE_TIME_FMT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE);

  private static final String C_ROOT = "details-dialog";
  private static final String C_HEADER_LEFT = "details-dialog__header-left";
  private static final String C_HEADER_RIGHT = "details-dialog__header-right";
  private static final String C_FORM = "details-dialog__form";
  private static final String C_URL = "details-dialog__url";
  private static final String C_EXPIRES_ROW = "details-dialog__expires-row";
  private static final String C_ALIAS_DLG = "details-dialog__alias-dialog";

  // i18n keys (Overview-leading)
  private static final String K_TITLE = "overview.details.title"; // Details: {0}

  private static final String K_F_SHORTCODE = "overview.details.field.shortcode";
  private static final String K_F_URL = "overview.details.field.originalUrl";
  private static final String K_F_CREATED = "overview.details.field.createdOn";
  private static final String K_F_EXPIRES = "overview.details.field.expires";
  private static final String K_F_ACTIVE = "overview.details.field.active";

  private static final String K_NO_EXPIRY = "overview.details.noExpiry"; // No expiry date

  private static final String K_BTN_OPEN = "overview.details.btn.open";
  private static final String K_BTN_COPY_SHORT = "overview.details.btn.copyShortUrl";
  private static final String K_BTN_COPY_URL = "overview.details.btn.copyUrl";
  private static final String K_BTN_DELETE = "overview.details.btn.delete";
  private static final String K_BTN_CLOSE = "common.close";

  private static final String K_BTN_EDIT = "overview.details.btn.edit";
  private static final String K_TT_EDIT = "overview.details.tt.edit";
  private static final String K_TT_SAVE = "overview.details.tt.save";
  private static final String K_TT_CANCEL = "overview.details.tt.cancel";

  private static final String K_VALID_URL_REQUIRED = "overview.details.validation.url.required"; // URL must not be blank

  private static final String K_ALIAS_BTN_ADD = "overview.details.alias.btn.add";
  private static final String K_ALIAS_TITLE = "overview.details.alias.title"; // New alias for: {0}
  private static final String K_ALIAS_BTN_SAVE = "common.save";
  private static final String K_ALIAS_BTN_CLOSE = "common.close";

  private final String shortCode;
  private final String originalUrl;
  private final Instant createdAt;
  private final Optional<Instant> expiresAt;
  private final Boolean active;

  private final TextField tfShort = new TextField();
  private final TextField tfUrl = new TextField();
  private final TextField tfCreated = new TextField();
  private final TextField tfExpires = new TextField();
  private final Checkbox cbActive = new Checkbox();
  private final Span statusPill = new Span();

  private final Button openBtn = new Button(new Icon(VaadinIcon.EXTERNAL_LINK));
  private final Button copyShortBtn = new Button(new Icon(VaadinIcon.COPY));
  private final Button copyUrlBtn = new Button(new Icon(VaadinIcon.COPY));
  private final Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
  private final Button closeBtn = new Button();

  private final Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
  private final Button saveBtn = new Button(new Icon(VaadinIcon.CHECK));
  private final Button cancelBtn = new Button(new Icon(VaadinIcon.CLOSE));
  private final DateTimePicker expiresField = new DateTimePicker();

  private final URLShortenerClient client;
  private final ShortUrlMapping item;
  private final Binder<ShortUrlMapping> binder = new Binder<>(ShortUrlMapping.class);

  public DetailsDialog(URLShortenerClient client, ShortUrlMapping mapping) {
    Objects.requireNonNull(client, "URLShortenerClient");
    Objects.requireNonNull(mapping, "ShortUrlMapping");

    this.client = client;
    this.item = mapping;

    this.shortCode = mapping.shortCode();
    this.originalUrl = mapping.originalUrl();
    this.createdAt = mapping.createdAt();
    this.expiresAt = mapping.expiresAt();
    this.active = mapping.active();

    addClassName(C_ROOT);

    setHeaderTitle(tr(K_TITLE, "Details: {0}", shortCode));
    setModality(ModalityMode.STRICT);
    setDraggable(true);
    setResizable(true);

    // Button styling
    openBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    editBtn.addThemeVariants(ButtonVariant.LUMO_WARNING);
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    saveBtn.setVisible(false);
    cancelBtn.setVisible(false);

    // i18n labels/texts
    applyI18n();

    // Binder
    binder.forField(tfUrl)
        .asRequired(tr(K_VALID_URL_REQUIRED, "URL must not be blank"))
        .withValidator((String url, ValueContext _) -> {
          var res = UrlValidator.validate(url);
          return res.valid() ? ValidationResult.ok() : ValidationResult.error(res.message());
        })
        .bind(ShortUrlMapping::originalUrl, (_, _) -> { });

    binder.forField(expiresField)
        .bind(
            m -> m.expiresAt()
                .map(i -> LocalDateTime.ofInstant(i, ZONE)).orElse(null),
            (_, _) -> { }
        );
    binder.readBean(item);

    // Build header content
    var leftHeader = new HorizontalLayout(openBtn, copyShortBtn, copyUrlBtn, deleteBtn);
    leftHeader.addClassName(C_HEADER_LEFT);
    leftHeader.setSpacing(true);
    leftHeader.setPadding(false);

    var rightHeader = new HorizontalLayout(editBtn, saveBtn, cancelBtn);
    rightHeader.addClassName(C_HEADER_RIGHT);
    rightHeader.setSpacing(true);
    rightHeader.setPadding(false);
    rightHeader.setAlignItems(FlexComponent.Alignment.CENTER);

    getHeader().add(leftHeader, rightHeader);

    configureFields();

    var form = new FormLayout();
    form.addClassName(C_FORM);
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("600px", 2)
    );
    form.add(tfShort, tfCreated, tfUrl, cbActive, tfExpires, buildExpiresRow(), statusPill);
    form.setColspan(tfUrl, 2);
    add(form);

    Button addAliasesBtn = new Button(tr(K_ALIAS_BTN_ADD, "Add aliases…"), _ -> {
      close();
      openAddAliasesDialog(mapping);
    });

    closeBtn.addClickListener(_ -> close());

    var footer = new HorizontalLayout(closeBtn, addAliasesBtn);
    footer.setSpacing(true);
    footer.setPadding(false);
    getFooter().add(footer);

    wireActions();
  }

  private void applyI18n() {
    tfShort.setLabel(tr(K_F_SHORTCODE, "Shortcode"));
    tfUrl.setLabel(tr(K_F_URL, "Original URL"));
    tfCreated.setLabel(tr(K_F_CREATED, "Created on"));
    tfExpires.setLabel(tr(K_F_EXPIRES, "Expires"));
    cbActive.setLabel(tr(K_F_ACTIVE, "Active"));

    openBtn.setText(tr(K_BTN_OPEN, "Open"));
    copyShortBtn.setText(tr(K_BTN_COPY_SHORT, "Copy ShortURL"));
    copyUrlBtn.setText(tr(K_BTN_COPY_URL, "Copy URL"));
    deleteBtn.setText(tr(K_BTN_DELETE, "Delete…"));
    closeBtn.setText(tr(K_BTN_CLOSE, "Close"));

    editBtn.setText(tr(K_BTN_EDIT, "Edit"));

    editBtn.getElement().setAttribute("title", tr(K_TT_EDIT, "Edit"));
    saveBtn.getElement().setAttribute("title", tr(K_TT_SAVE, "Save"));
    cancelBtn.getElement().setAttribute("title", tr(K_TT_CANCEL, "Cancel"));

    expiresField.setLabel(tr(K_F_EXPIRES, "Expires"));
  }

  private void configureFields() {
    tfShort.setValue(shortCode);
    tfShort.setReadOnly(true);

    tfUrl.addClassName(C_URL);
    tfUrl.setValue(originalUrl);
    tfUrl.setReadOnly(true);
    tfUrl.getElement().setProperty("title", originalUrl);

    cbActive.setValue(active);
    cbActive.setReadOnly(true);

    tfCreated.setValue(DATE_TIME_FMT.format(createdAt));
    tfCreated.setReadOnly(true);

    tfExpires.setValue(expiresAt.map(DATE_TIME_FMT::format).orElse(tr(K_NO_EXPIRY, "No expiry date")));
    tfExpires.setReadOnly(true);

    statusPill.getElement().getThemeList().add("badge");
    statusPill.getElement().getThemeList().add("pill");
    statusPill.getElement().getThemeList().add("small");
    var statusText = computeStatusText(expiresAt);
    statusPill.setText(statusText.text());
    statusPill.getElement().getThemeList().add(statusText.theme());
  }

  private Component buildExpiresRow() {
    expiresField.setStep(Duration.ofMinutes(30));
    expiresField.setWidthFull();
    expiresField.setVisible(false);
    expiresField.setWeekNumbersVisible(true);
    var datePickerI18n = new DatePicker.DatePickerI18n()
        .setFirstDayOfWeek(1);
    expiresField.setDatePickerI18n(datePickerI18n);
    var dateTimePickerI18n = new DateTimePicker.DateTimePickerI18n();
    expiresField.setI18n(dateTimePickerI18n);

    HorizontalLayout row = new HorizontalLayout(expiresField);
    row.addClassName(C_EXPIRES_ROW);
    row.setSpacing(true);
    row.setPadding(false);
    row.setWidthFull();
    row.setFlexGrow(1, expiresField);
    return row;
  }

  private void wireActions() {
    openBtn.addClickListener(_ -> {
      fireEvent(new OpenEvent(this, shortCode, originalUrl));
      getUI().ifPresent(ui -> ui.getPage().open(originalUrl, "_blank"));
    });

    copyShortBtn.addClickListener(_ -> {
      var shortURL = SHORTCODE_BASE_URL + shortCode;
      copyToClipboard(shortURL);
      fireEvent(new CopyShortcodeEvent(this, shortURL));
      Notifications.shortCodeCopied();
    });

    copyUrlBtn.addClickListener(_ -> {
      copyToClipboard(originalUrl);
      fireEvent(new CopyUrlEvent(this, originalUrl));
      Notifications.urlCopied();
    });

    deleteBtn.addClickListener(_ -> fireEvent(new DeleteEvent(this, shortCode)));

    editBtn.addClickListener(_ -> switchToEdit(true));
    saveBtn.addClickListener(_ -> onSave());

    cancelBtn.addClickListener(_ -> {
      binder.readBean(item);
      switchToEdit(false);
    });
  }

  private void switchToEdit(boolean enable) {
    tfUrl.setReadOnly(!enable);
    tfExpires.setVisible(!enable);
    cbActive.setReadOnly(!enable);

    expiresField.setVisible(enable);
    expiresField.setReadOnly(!enable);

    editBtn.setVisible(!enable);
    saveBtn.setVisible(enable);
    cancelBtn.setVisible(enable);

    if (enable) {
      tfUrl.focus();
      tfUrl.getElement().executeJs("this.select()");
    }
  }

  private void onSave() {
    try {
      String newUrl = tfUrl.getValue();
      var validate = UrlValidator.validate(newUrl);
      if (!validate.valid()) throw new RuntimeException(validate.message());

      LocalDateTime ldt = expiresField.getValue();
      Instant expires = (ldt == null) ? null : ldt.atZone(ZoneId.systemDefault()).toInstant();

      boolean ok = client.edit(item.shortCode(), newUrl, expires, cbActive.getValue());
      if (ok) {
        Notifications.saved();
        fireEvent(new SavedEvent(this, item.shortCode()));
        switchToEdit(false);
        close();
      } else {
        Notifications.noChanges();
      }
    } catch (Exception ex) {
      logger().error("Save failed", ex);
      Notifications.operationFailed(ex);
    }
  }

  private void openAddAliasesDialog(ShortUrlMapping currentMapping) {
    var client = UrlShortenerClientFactory.newInstance();

    Dialog dlg = new Dialog();
    dlg.addClassName(C_ALIAS_DLG);
    dlg.setHeaderTitle(tr(K_ALIAS_TITLE, "New alias for: {0}", currentMapping.shortCode()));
    dlg.setModality(ModalityMode.STRICT);
    dlg.setCloseOnEsc(true);
    dlg.setCloseOnOutsideClick(false);

    var editor = new MultiAliasEditorStrict(
        SHORTCODE_BASE_URL,
        alias -> {
          try {
            return client.resolveShortcode(alias) == null;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
    );
    editor.setWidthFull();

    Button saveBtn = new Button(tr(K_ALIAS_BTN_SAVE, "Save"), _ -> {
      editor.validateAll();
      var validAliases = editor.getValidAliases();
      if (validAliases.isEmpty()) {
        Notifications.noValidShortCode();
        return;
      }

      int ok = 0;
      for (String alias : validAliases) {
        try {
          var expires = currentMapping.expiresAt().orElse(null);
          client.createCustomMapping(alias, currentMapping.originalUrl(), expires, currentMapping.active());
          editor.markSaved(alias);
          ok++;
        } catch (Exception ex) {
          editor.markError(alias, String.valueOf(ex.getMessage()));
        }
      }
      Notifications.savedAndNotSaved(ok, editor.countOpen());
      refreshAfterAliasAdd();
    });

    Button closeBtn = new Button(tr(K_ALIAS_BTN_CLOSE, "Close"), _ -> dlg.close());

    dlg.addOpenedChangeListener(e -> {
      if (!e.isOpened()) {
        var ui = UI.getCurrent();
        if (ui != null) {
          ComponentUtil.fireEvent(ui, new MappingCreatedOrChanged(this));
        }
        DetailsDialog.this.open();
      }
    });

    dlg.add(editor);
    dlg.getFooter().add(saveBtn, closeBtn);
    dlg.open();
  }

  private void refreshAfterAliasAdd() {
    var ui = UI.getCurrent();
    if (ui != null) {
      ComponentUtil.fireEvent(ui, new MappingCreatedOrChanged(this));
    }
  }

  public Registration addOpenListener(ComponentEventListener<OpenEvent> l) {
    return addListener(OpenEvent.class, l);
  }

  public Registration addCopyShortListener(ComponentEventListener<CopyShortcodeEvent> l) {
    return addListener(CopyShortcodeEvent.class, l);
  }

  public Registration addCopyUrlListener(ComponentEventListener<CopyUrlEvent> l) {
    return addListener(CopyUrlEvent.class, l);
  }

  public Registration addDeleteListener(ComponentEventListener<DeleteEvent> l) {
    return addListener(DeleteEvent.class, l);
  }

  public Registration addSavedListener(ComponentEventListener<SavedEvent> listener) {
    return addListener(SavedEvent.class, listener);
  }

  public static class SavedEvent extends ComponentEvent<DetailsDialog> {
    private final String shortCode;

    public SavedEvent(DetailsDialog src, String shortCode) {
      super(src, false);
      this.shortCode = shortCode;
    }

    public String getShortCode() {
      return shortCode;
    }
  }

  public static class DetailsEvent extends ComponentEvent<DetailsDialog> {
    public DetailsEvent(DetailsDialog source) {
      super(source, false);
    }
  }

  public static class OpenEvent extends DetailsEvent {
    public final String shortCode;
    public final String originalUrl;

    public OpenEvent(DetailsDialog src, String sc, String url) {
      super(src);
      this.shortCode = sc;
      this.originalUrl = url;
    }
  }

  public static class CopyShortcodeEvent extends DetailsEvent {
    public final String shortCode;

    public CopyShortcodeEvent(DetailsDialog src, String sc) {
      super(src);
      this.shortCode = sc;
    }
  }

  public static class CopyUrlEvent extends DetailsEvent {
    public final String url;

    public CopyUrlEvent(DetailsDialog src, String url) {
      super(src);
      this.url = url;
    }
  }

  public static class DeleteEvent extends DetailsEvent {
    public final String shortCode;

    public DeleteEvent(DetailsDialog src, String sc) {
      super(src);
      this.shortCode = sc;
    }
  }
}