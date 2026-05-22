package com.svenruppert.urlshortener.api.handler.statistics.imports;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsStore;
import com.svenruppert.urlshortener.api.utils.ErrorResponses;
import com.svenruppert.urlshortener.api.utils.RequestMethodUtils;
import com.svenruppert.urlshortener.api.utils.SuccessResponses;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.statistics.StatisticsImportResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import static com.svenruppert.urlshortener.api.utils.QueryUtils.first;
import static com.svenruppert.urlshortener.api.utils.QueryUtils.parseQueryParams;
import static com.svenruppert.urlshortener.core.DefaultValues.STATISTICS_IMPORT_MAX_NDJSON_BYTES;

public class StatisticsImportHandler
    implements HttpHandler, HasLogger {

  private final StatisticsStore store;

  public StatisticsImportHandler(StatisticsStore store) {
    this.store = store;
  }

  @Override
  public void handle(HttpExchange ex)
      throws IOException {
    if (!RequestMethodUtils.requirePost(ex)) return;

    var query = parseQueryParams(ex.getRequestURI().getRawQuery());
    String mode = first(query, "mode");
    boolean replace = "replace".equalsIgnoreCase(mode);

    LocalDate replaceFrom = null;
    LocalDate replaceTo = null;
    if (replace) {
      try {
        String fromRaw = first(query, "from");
        String toRaw = first(query, "to");
        if (fromRaw == null || toRaw == null) {
          ErrorResponses.badRequest(ex, "missing_parameter",
                                    "mode=replace requires from and to query parameters");
          return;
        }
        replaceFrom = LocalDate.parse(fromRaw);
        replaceTo = LocalDate.parse(toRaw);
        if (replaceFrom.isAfter(replaceTo)) {
          ErrorResponses.badRequest(ex, "invalid_range", "from must not be after to");
          return;
        }
      } catch (Exception e) {
        ErrorResponses.badRequest(ex, "invalid_date",
                                  "Invalid date format. Use ISO format: yyyy-MM-dd");
        return;
      }
    }

    try (InputStream body = ex.getRequestBody()) {
      if (replace) {
        long deleted = store.deleteEventsInRange(null, replaceFrom, replaceTo);
        logger().info("replace mode: deleted {} existing events in [{}..{}]",
                      deleted, replaceFrom, replaceTo);
      }

      var reader = new StatisticsImportReader(store, STATISTICS_IMPORT_MAX_NDJSON_BYTES);
      StatisticsImportReader.Result result = reader.importFrom(body);

      if (result.minDate() != null && result.maxDate() != null) {
        long buckets = store.reaggregate(result.minDate(), result.maxDate());
        logger().info("Reaggregated {} buckets after import", buckets);
      }

      StatisticsImportResponse response = new StatisticsImportResponse(
          replace ? "replace" : "append",
          result.importedEvents(),
          result.skippedLines(),
          result.minDate(),
          result.maxDate()
      );
      SuccessResponses.okJson(ex, JsonUtils.toJson(response));

    } catch (IOException e) {
      logger().warn("Statistics import failed", e);
      ErrorResponses.badRequest(ex, "import_failed",
                                e.getMessage() != null ? e.getMessage() : "import failed");
    } catch (Exception e) {
      logger().error("Statistics import error", e);
      ErrorResponses.internalServerError(ex, e);
    }
  }
}
