package com.svenruppert.urlshortener.ui.vaadin.views;



import com.svenruppert.urlshortener.ui.vaadin.tools.I18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class Notifications {

  private static final int DEFAULT_DURATION = 3000;
  private static final Notification.Position DEFAULT_POSITION =
      Notification.Position.BOTTOM_START;

  private Notifications() {
  }

  private static void show(String message,
                           NotificationVariant variant,
                           int duration,
                           Notification.Position position) {
    Notification n = new Notification(message, duration, position);
    if (variant != null) n.addThemeVariants(variant);
    n.open();
  }

  private static void success(String key, String fallback, Object... params) {
    show(I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_SUCCESS,
         DEFAULT_DURATION,
         DEFAULT_POSITION);
  }

  private static void error(String key, String fallback, Object... params) {
    show(I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_ERROR,
         DEFAULT_DURATION,
         DEFAULT_POSITION);
  }

  private static void warning(String key, String fallback, Object... params) {
    show(I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_WARNING,
         DEFAULT_DURATION,
         DEFAULT_POSITION);
  }

  private static void info(String key, String fallback, Object... params) {
    show(I18n.tr(key, fallback, params),
         null,
         DEFAULT_DURATION,
         DEFAULT_POSITION);
  }

  // =========================
  // i18n / generic API
  // =========================

  public static void infoKey(String key, String fallback, Object... params) {
    show(com.svenruppert.urlshortener.ui.vaadin.tools.I18n.tr(key, fallback, params),
         null, DEFAULT_DURATION, DEFAULT_POSITION);
  }

  public static void successKey(String key, String fallback, Object... params) {
    show(com.svenruppert.urlshortener.ui.vaadin.tools.I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_SUCCESS, DEFAULT_DURATION, DEFAULT_POSITION);
  }

  public static void warningKey(String key, String fallback, Object... params) {
    show(com.svenruppert.urlshortener.ui.vaadin.tools.I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_WARNING, DEFAULT_DURATION, DEFAULT_POSITION);
  }

  public static void errorKey(String key, String fallback, Object... params) {
    show(com.svenruppert.urlshortener.ui.vaadin.tools.I18n.tr(key, fallback, params),
         NotificationVariant.LUMO_ERROR, DEFAULT_DURATION, DEFAULT_POSITION);
  }

  public static void saved() {
    success("notifications.saved", "Saved successfully");
  }

  public static void savedAndNotSaved(long success, long failed) {
    success("notifications.savedAndFailed", "Saved: {0} | Failed: {1}", success, failed);
  }

  public static void updatedAndNotUpdated(long success, long failed) {
    success("notifications.updatedAndFailed", "Updated: {0} | Failed: {1}", success, failed);
  }

  public static void deletedAndNotDeleted(long success, long failed) {
    success("notifications.deletedAndFailed", "Deleted: {0} | Failed: {1}", success, failed);
  }

  public static void noChanges() {
    info("notifications.noChanges", "No changes detected");
  }

  public static void noValidShortCode() {
    warning("notifications.noValidShortCode", "No valid short code");
  }

  public static void noDateSelected() {
    warning("notifications.noDateSelected", "No date selected");
  }

  public static void noSelection() {
    warning("notifications.noSelection", "No entries selected");
  }

  public static void loadingFailed() {
    error("notifications.loadingFailed", "Loading failed");
  }

  public static void shortCodeDeleted() {
    success("notifications.shortCodeDeleted", "Short code deleted");
  }

  public static void shortCodeNotFound() {
    warning("notifications.shortCodeNotFound", "Short code not found");
  }

  public static void shortCodeCopied() {
    info("notifications.shortCodeCopied", "Short code copied");
  }

  public static void urlCopied() {
    info("notifications.urlCopied", "URL copied");
  }

  public static void statusUpdatedOK() {
    success("notifications.statusUpdated", "Status updated");
  }

  public static void statusUpdatedFailed(Exception ex) {
    error("notifications.statusUpdatedFailed", "Error updating status: {0}", safeMessage(ex));
  }

  public static void operationFailed(Exception ex) {
    error("notifications.operationFailed", "Operation failed: {0}", safeMessage(ex));
  }

  public static void loginCurrentlyDisabled() {
    warning("notifications.loginDisabled",
            "Login is currently disabled. Check server configuration.");
  }

  public static void loginCurrentlyNotConfigured() {
    warning("notifications.loginNotConfigured",
            "Login is not configured. Verify configuration file.");
  }

  private static String safeMessage(Exception ex) {
    return (ex == null || ex.getMessage() == null)
        ? I18n.tr("notifications.unknownError", "Unknown error")
        : ex.getMessage();
  }
}