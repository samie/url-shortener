package com.svenruppert.urlshortener.ui.vaadin.components;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18n;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.svenruppert.urlshortener.ui.vaadin.views.overview.DetailsDialog.ZONE;

public class ExpiryBadgeFactory
    implements HasLogger {

  private ExpiryBadgeFactory() {
  }

  /**
   * Computes the expiry status label and its theme colour.
   */
  public static Status computeStatusText(Optional<Instant> expiresAt) {

    return expiresAt
        .map(ts -> {
          var now = LocalDate.now(ZONE);
          var expiresAtThisZone = ts.atZone(ZONE).toLocalDate();
          long d = ChronoUnit.DAYS.between(now, expiresAtThisZone);
          HasLogger.staticLogger().info("computeStatusText delta [days] = " + d);
          if (d < 0) {
            return new Status(
                I18n.tr("expiry.expired", "Expired"),
                "error"
            );
          }

          if (d == 0) {
            return new Status(
                I18n.tr("expiry.today", "Expires today"),
                "warning"
            );
          }

          if (d == 1) {
            return new Status(
                I18n.tr("expiry.inOneDay", "Expires in 1 day"),
                "warning"
            );
          }

          if (d == 2) {
            return new Status(
                I18n.tr("expiry.inDays", "Expires in {0} days", d),
                "warning"
            );
          }

          return new Status(
              I18n.tr("expiry.validDaysLeft", "Valid ({0} days left)", d),
              "success"
          );

        })
        .orElse(new Status(
            I18n.tr("expiry.none", "No expiry"),
            "contrast"
        ));
  }

  public record Status(String text, String theme) { }
}