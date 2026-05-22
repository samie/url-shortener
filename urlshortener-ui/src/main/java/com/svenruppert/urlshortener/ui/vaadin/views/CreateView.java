package com.svenruppert.urlshortener.ui.vaadin.views;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.urlmapping.ShortenRequest;
import com.svenruppert.urlshortener.core.validation.UrlValidator;
import com.svenruppert.urlshortener.ui.vaadin.MainLayout;
import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.VisibleFor;
import com.svenruppert.urlshortener.ui.vaadin.components.MultiAliasEditorStrict;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.tools.UrlShortenerClientFactory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.svenruppert.urlshortener.core.DefaultValues.SHORTCODE_BASE_URL;

@Route(value = CreateView.PATH, layout = MainLayout.class)
@VisibleFor(AppRole.USER)
@CssImport("./styles/create-view.css")
public class CreateView extends VerticalLayout implements HasLogger, I18nSupport {

  public static final String PATH = "create";
  private static final ZoneId ZONE = ZoneId.systemDefault();

  // class names
  private static final String C_ROOT = "create-root";
  private static final String C_SPLIT = "create-split";
  private static final String C_COL = "create-col";
  private static final String C_FORM = "create-form";
  private static final String C_ACTIONS = "create-actions";
  private static final String C_EDITOR_WRAP = "create-editor";

  private final URLShortenerClient urlShortenerClient = UrlShortenerClientFactory.newInstance();

  // i18n keys
  private static final String K_TITLE_LEFT = "create.title.left";
  private static final String K_TITLE_RIGHT = "create.title.right";

  private static final String K_FIELD_URL = "create.field.url";
  private static final String K_FIELD_EXPIRES_DATE = "create.field.expiresDate";
  private static final String K_FIELD_NO_EXPIRY = "create.field.noExpiry";
  private static final String K_FIELD_ACTIVE = "create.field.active";

  private static final String K_BTN_SAVE = "common.save";
  private static final String K_BTN_RESET = "common.reset";

  private static final String K_VALID_URL_REQUIRED = "create.validation.url.required";

  private static final String K_TOAST_URL_EMPTY = "create.toast.urlEmpty";
  private static final String K_TOAST_NO_ALIASES = "create.toast.noAliases";
  private static final String K_TOAST_EXPIRES_FUTURE = "create.toast.expiresFuture";
  private static final String K_TOAST_SAVED = "create.toast.saved"; // Saved: {0} | Open: {1}

  private final TextField urlField = new TextField();
  private final Button saveAllButton = new Button();
  private final Button resetButton = new Button();

  private final DateTimePicker expiresDateTime = new DateTimePicker();
  private final Checkbox noExpiry = new Checkbox();
  private final Checkbox cbActive = new Checkbox();

  public CreateView() {
    addClassName(C_ROOT);

    setSpacing(true);
    setPadding(true);
    setSizeFull();

    applyI18n();

    urlField.setWidthFull();
    urlField.addValueChangeListener(ev -> {
      final String v = ev.getValue();
      if (v != null && v.startsWith("http://")) {
        urlField.setHelperText("⚠ Insecure HTTP – data transmitted unencrypted");
      } else {
        urlField.setHelperText(null);
      }
    });
    saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    configureExpiryFields();

    FormLayout form = new FormLayout();
    form.addClassName(C_FORM);
    form.add(urlField, noExpiry, cbActive, expiresDateTime);
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("900px", 2)
    );
    form.setColspan(urlField, 2);

    HorizontalLayout actions = new HorizontalLayout(saveAllButton, resetButton);
    actions.addClassName(C_ACTIONS);
    actions.setWidthFull();
    actions.setJustifyContentMode(JustifyContentMode.START);

    Binder<ShortenRequest> binder = new Binder<>(ShortenRequest.class);

    binder.forField(urlField)
        .asRequired(tr(K_VALID_URL_REQUIRED, "URL must not be empty"))
        .withValidator((String url, ValueContext _) -> {
          var res = UrlValidator.validate(url);
          return res.valid() ? ValidationResult.ok() : ValidationResult.error(res.message());
        })
        .bind(ShortenRequest::getUrl, ShortenRequest::setUrl);

    var editor = new MultiAliasEditorStrict(
        SHORTCODE_BASE_URL,
        alias -> {
          try {
            return urlShortenerClient.resolveShortcode(alias) == null;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
    );
    editor.setSizeFull();
    editor.addClassName(C_EDITOR_WRAP);

    saveAllButton.addClickListener(_ -> {
      var validated = binder.validate();
      if (validated.hasErrors()) return;
      if (!validateExpiryInFuture()) return;

      if (urlField.getValue() == null || urlField.getValue().isBlank()) {
        Notification.show(tr(K_TOAST_URL_EMPTY, "Target URL is empty"), 2500, Notification.Position.TOP_CENTER);
        return;
      }

      editor.validateAll();
      List<String> validAliases = editor.getValidAliases();
      if (validAliases.isEmpty()) {
        Notification.show(tr(K_TOAST_NO_ALIASES, "No valid aliases to save"), 2000, Notification.Position.TOP_CENTER);
        return;
      }

      Optional<Instant> expiresAt = computeExpiresAt();

      int ok = 0;
      for (String alias : validAliases) {
        try {
          var activeState = cbActive.getValue();
          var customMapping = urlShortenerClient.createCustomMapping(
              alias,
              urlField.getValue(),
              expiresAt.orElse(null),
              activeState
          );
          if (customMapping != null) editor.markSaved(alias);
          ok++;
        } catch (Exception ex) {
          editor.markError(alias, String.valueOf(ex.getMessage()));
        }
      }

      Notification.show(
          tr(K_TOAST_SAVED, "Saved: {0} | Open: {1}", ok, editor.countOpen()),
          3500,
          Notification.Position.TOP_CENTER
      );
    });

    resetButton.addClickListener(_ -> {
      clearFormAll(binder);
      editor.clearAllRows();
    });

    // --- SplitLayout
    var leftCol = new VerticalLayout(new H2(tr(K_TITLE_LEFT, "Create new short links")), form, actions);
    leftCol.addClassName(C_COL);
    leftCol.setPadding(false);
    leftCol.setSpacing(true);
    leftCol.setSizeFull();

    var rightCol = new VerticalLayout(new H2(tr(K_TITLE_RIGHT, "Aliases")), editor);
    rightCol.addClassName(C_COL);
    rightCol.setPadding(false);
    rightCol.setSpacing(true);
    rightCol.setSizeFull();

    SplitLayout split = new SplitLayout(leftCol, rightCol);
    split.addClassName(C_SPLIT);
    split.setSizeFull();
    split.setSplitterPosition(40);

    add(split);
  }

  private void applyI18n() {
    urlField.setLabel(tr(K_FIELD_URL, "Target URL"));

    saveAllButton.setText(tr(K_BTN_SAVE, "Save"));
    resetButton.setText(tr(K_BTN_RESET, "Reset"));

    expiresDateTime.setLabel(tr(K_FIELD_EXPIRES_DATE, "Expires (date)"));
    noExpiry.setLabel(tr(K_FIELD_NO_EXPIRY, "No expiry"));
    cbActive.setLabel(tr(K_FIELD_ACTIVE, "Active"));
  }

  private void configureExpiryFields() {
    expiresDateTime.setStep(java.time.Duration.ofMinutes(30));
    expiresDateTime.setWeekNumbersVisible(true);
    noExpiry.addValueChangeListener(ev -> {
      boolean disabled = ev.getValue();
      expiresDateTime.setEnabled(!disabled);
      if (disabled) {
        expiresDateTime.clear();
      }
    });
  }

  private Optional<Instant> computeExpiresAt() {
    if (Boolean.TRUE.equals(noExpiry.getValue())) {
      return Optional.empty();
    }
    final java.time.LocalDateTime ldt = expiresDateTime.getValue();
    if (ldt == null) {
      return Optional.empty();
    }
    return Optional.of(ldt.atZone(ZONE).toInstant());
  }

  private boolean validateExpiryInFuture() {
    var exp = computeExpiresAt();
    if (exp.isPresent() && exp.get().isBefore(Instant.now())) {
      Notification.show(tr(K_TOAST_EXPIRES_FUTURE, "Expiry must be in the future"));
      return false;
    }
    return true;
  }

  private void clearFormAll(Binder<ShortenRequest> binder) {
    urlField.clear();
    noExpiry.clear();
    expiresDateTime.clear();
    binder.setBean(new ShortenRequest(null, null, null, null));
    urlField.setInvalid(false);
  }
}