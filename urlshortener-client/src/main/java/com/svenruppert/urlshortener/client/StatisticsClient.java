package com.svenruppert.urlshortener.client;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.statistics.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static com.svenruppert.urlshortener.core.DefaultValues.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Client for accessing redirect statistics via the Statistics API.
 * Provides methods to query counts, hourly/daily aggregates, timelines, and configuration.
 */
public class StatisticsClient
    implements HasLogger {

  private static final int CONNECT_TIMEOUT = 3_000;
  private static final int READ_TIMEOUT = 5_000;

  private final URI serverBaseAdmin;

  /**
   * Creates a client with default admin server URL.
   */
  public StatisticsClient() {
    this(ADMIN_SERVER_URL);
  }

  /**
   * Creates a client with custom admin server URL.
   */
  public StatisticsClient(String serverBaseUrlAdmin) {
    var urlToServerAdmin = serverBaseUrlAdmin.endsWith("/")
        ? serverBaseUrlAdmin
        : serverBaseUrlAdmin + "/";
    logger().info("StatisticsClient initialized with server URL: {}", urlToServerAdmin);
    this.serverBaseAdmin = URI.create(urlToServerAdmin);
  }

  // ============================================================================
  // Count Queries
  // ============================================================================

  /**
   * Gets the total redirect count for a shortCode (all time).
   *
   * @param shortCode the short code to query
   * @return the total count response
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if shortCode is null or blank
   */
  public StatisticsCountResponse getTotalCount(String shortCode)
      throws IOException {
    validateShortCode(shortCode);
    String path = PATH_ADMIN_STATISTICS_COUNT + "/" + shortCode;
    return executeGet(path, StatisticsCountResponse.class);
  }

  /**
   * Gets the redirect count for a shortCode on a specific date.
   *
   * @param shortCode the short code to query
   * @param date      the date to query
   * @return the count response for that date
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if shortCode is null or blank
   */
  public StatisticsCountResponse getCountForDate(String shortCode, LocalDate date)
      throws IOException {
    validateShortCode(shortCode);
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    String path = PATH_ADMIN_STATISTICS_COUNT + "/" + shortCode + "?date=" + date;
    return executeGet(path, StatisticsCountResponse.class);
  }

  /**
   * Gets the redirect count for a shortCode within a date range (inclusive).
   *
   * @param shortCode the short code to query
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return the count response for the date range
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if any parameter is null or blank
   */
  public StatisticsCountResponse getCountForDateRange(String shortCode, LocalDate from, LocalDate to)
      throws IOException {
    validateShortCode(shortCode);
    if (from == null || to == null) {
      throw new IllegalArgumentException("from and to must not be null");
    }
    String path = PATH_ADMIN_STATISTICS_COUNT + "/" + shortCode + "?from=" + from + "&to=" + to;
    return executeGet(path, StatisticsCountResponse.class);
  }

  // ============================================================================
  // Hourly Statistics (Hot Window only)
  // ============================================================================

  /**
   * Gets hourly statistics (24-hour breakdown) for a shortCode on a specific date.
   * Only available for dates within the hot window.
   *
   * @param shortCode the short code to query
   * @param date      the date to query
   * @return optional containing hourly statistics, empty if date is outside hot window
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if shortCode is null or blank
   */
  public Optional<HourlyStatisticsResponse> getHourlyStatistics(String shortCode, LocalDate date)
      throws IOException {
    validateShortCode(shortCode);
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    String path = PATH_ADMIN_STATISTICS_HOURLY + "/" + shortCode + "?date=" + date;
    return executeGetOptional(path, HourlyStatisticsResponse.class);
  }

  // ============================================================================
  // Daily Statistics
  // ============================================================================

  /**
   * Gets daily statistics for a shortCode on a specific date.
   *
   * @param shortCode the short code to query
   * @param date      the date to query
   * @return optional containing daily statistics, empty if no data exists
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if shortCode is null or blank
   */
  public Optional<DailyStatisticsResponse> getDailyStatistics(String shortCode, LocalDate date)
      throws IOException {
    validateShortCode(shortCode);
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    String path = PATH_ADMIN_STATISTICS_DAILY + "/" + shortCode + "?date=" + date;
    return executeGetOptional(path, DailyStatisticsResponse.class);
  }

  // ============================================================================
  // Timeline Queries
  // ============================================================================

  /**
   * Gets a timeline of daily counts for a shortCode within a date range.
   *
   * @param shortCode the short code to query
   * @param from      start date (inclusive)
   * @param to        end date (inclusive)
   * @return timeline response with daily counts
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if any parameter is null or blank
   */
  public StatisticsTimelineResponse getTimeline(String shortCode, LocalDate from, LocalDate to)
      throws IOException {
    validateShortCode(shortCode);
    if (from == null || to == null) {
      throw new IllegalArgumentException("from and to must not be null");
    }
    String path = PATH_ADMIN_STATISTICS_TIMELINE + "/" + shortCode + "?from=" + from + "&to=" + to;
    return executeGet(path, StatisticsTimelineResponse.class);
  }

  // ============================================================================
  // Configuration
  // ============================================================================

  /**
   * Gets the current statistics configuration.
   *
   * @return the current configuration
   * @throws IOException if the request fails
   */
  public StatisticsConfigResponse getConfig()
      throws IOException {
    return executeGet(PATH_ADMIN_STATISTICS_CONFIG, StatisticsConfigResponse.class);
  }

  /**
   * Updates the statistics configuration.
   *
   * @param hotWindowDays             number of days in the hot window (minimum 1)
   * @param writerBatchSize           batch size for the writer thread (minimum 1)
   * @param aggregatorIntervalSeconds interval for aggregator thread (minimum 60)
   * @param statisticsEnabled         whether statistics are enabled
   * @return the updated configuration
   * @throws IOException              if the request fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public StatisticsConfigResponse updateConfig(
      int hotWindowDays,
      int writerBatchSize,
      int aggregatorIntervalSeconds,
      boolean statisticsEnabled
  )
      throws IOException {
    if (hotWindowDays < 1) {
      throw new IllegalArgumentException("hotWindowDays must be at least 1");
    }
    if (writerBatchSize < 1) {
      throw new IllegalArgumentException("writerBatchSize must be at least 1");
    }
    if (aggregatorIntervalSeconds < 60) {
      throw new IllegalArgumentException("aggregatorIntervalSeconds must be at least 60");
    }

    String jsonBody = String.format(
        "{\"hotWindowDays\":%d,\"writerBatchSize\":%d,\"aggregatorIntervalSeconds\":%d,\"statisticsEnabled\":%b}",
        hotWindowDays, writerBatchSize, aggregatorIntervalSeconds, statisticsEnabled
    );

    return executePut(PATH_ADMIN_STATISTICS_CONFIG, jsonBody, StatisticsConfigResponse.class);
  }

  /**
   * Enables or disables statistics collection.
   *
   * @param enabled true to enable, false to disable
   * @return the updated configuration
   * @throws IOException if the request fails
   */
  public StatisticsConfigResponse setStatisticsEnabled(boolean enabled)
      throws IOException {
    StatisticsConfigResponse current = getConfig();
    return updateConfig(
        current.hotWindowDays(),
        current.writerBatchSize(),
        current.aggregatorIntervalSeconds(),
        enabled
    );
  }

  // ============================================================================
  // Export
  // ============================================================================

  /**
   * Streams the statistics export ZIP. The caller is responsible for closing
   * the returned stream; closing it also releases the HTTP connection.
   *
   * @param from       inclusive lower bound, or {@code null} for "earliest available"
   * @param to         inclusive upper bound, or {@code null} for "today"
   * @param shortCodes restrict the export to these codes; {@code null}/empty = all
   */
  public InputStream exportAsZipStream(LocalDate from, LocalDate to, Collection<String> shortCodes)
      throws IOException {
    String path = buildExportPath(from, to, shortCodes, null);
    return fetchZipStream(path);
  }

  /**
   * Resolves the server-provided filename via HEAD and returns a download
   * descriptor that can re-open the ZIP stream as often as required.
   */
  public ExportZipDownload exportAsZipDownload(LocalDate from, LocalDate to, Collection<String> shortCodes)
      throws IOException {
    String basePath = buildExportPath(from, to, shortCodes, null);
    HeadExportInfo info = resolveExportFilenameViaHead(basePath);
    String getPath = buildExportPath(from, to, shortCodes, info.exportTsIso());
    Supplier<InputStream> factory = () -> {
      try {
        return fetchZipStream(getPath);
      } catch (IOException e) {
        throw new RuntimeException("Failed to open statistics export ZIP stream", e);
      }
    };
    return new ExportZipDownload(info.filename(), factory);
  }

  /**
   * Downloads the statistics export ZIP into {@code targetDirectory} and
   * returns the resulting file path. The filename is taken from the server's
   * {@code Content-Disposition} header.
   */
  public Path exportAsZipFile(LocalDate from, LocalDate to, Collection<String> shortCodes, Path targetDirectory)
      throws IOException {
    if (targetDirectory == null) {
      throw new IllegalArgumentException("targetDirectory must not be null");
    }
    String path = buildExportPath(from, to, shortCodes, null);
    URI uri = serverBaseAdmin.resolve(path);
    HttpURLConnection con = openConnection(uri, "GET", APPLICATION_ZIP, null);
    try {
      int code = con.getResponseCode();
      if (code != 200) {
        String err = readErrorBody(con, code);
        throw new IOException("HTTP " + code + " for GET " + uri + " body=" + err);
      }
      Files.createDirectories(targetDirectory);
      String fallback = STATISTICS_EXPORT_FILE_NAME + ".zip";
      String filename = sanitizeFilename(extractFilenameFromContentDisposition(
          con.getHeaderField(CONTENT_DISPOSITION), fallback), fallback);
      Path normalizedDir = targetDirectory.normalize();
      Path target = normalizedDir.resolve(filename).normalize();
      if (!target.startsWith(normalizedDir)) {
        throw new IOException("Refusing to write outside target directory: " + target);
      }
      try (InputStream in = con.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
      }
    } finally {
      con.disconnect();
    }
  }

  // ============================================================================
  // Import
  // ============================================================================

  /**
   * Imports a statistics ZIP from the given stream. The stream is consumed in
   * full and uploaded to the server. The server replays the events into its
   * writer pipeline and rebuilds aggregates for the imported date range.
   */
  public StatisticsImportResponse importZip(InputStream zipStream)
      throws IOException {
    if (zipStream == null) {
      throw new IllegalArgumentException("zipStream must not be null");
    }
    return importZip(zipStream.readAllBytes(), null, null, null);
  }

  /**
   * Imports a statistics ZIP from raw bytes.
   */
  public StatisticsImportResponse importZip(byte[] zipBytes)
      throws IOException {
    return importZip(zipBytes, null, null, null);
  }

  /**
   * Imports a statistics ZIP using replace semantics: existing events within
   * {@code from..to} are deleted before the new events are written.
   */
  public StatisticsImportResponse importZipReplace(byte[] zipBytes, LocalDate from, LocalDate to)
      throws IOException {
    if (from == null || to == null) {
      throw new IllegalArgumentException("from and to are required for replace mode");
    }
    return importZip(zipBytes, "replace", from, to);
  }

  private StatisticsImportResponse importZip(byte[] zipBytes, String mode, LocalDate from, LocalDate to)
      throws IOException {
    if (zipBytes == null || zipBytes.length == 0) {
      throw new IllegalArgumentException("zipBytes is null/empty");
    }
    StringBuilder qs = new StringBuilder();
    if (mode != null) {
      qs.append("mode=").append(URLEncoder.encode(mode, UTF_8));
      if (from != null) qs.append("&from=").append(from);
      if (to != null) qs.append("&to=").append(to);
    }
    String path = PATH_ADMIN_STATISTICS_IMPORT + (qs.isEmpty() ? "" : "?" + qs);
    URI uri = serverBaseAdmin.resolve(path);

    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
    con.setRequestMethod("POST");
    con.setConnectTimeout(CONNECT_TIMEOUT);
    con.setReadTimeout(READ_TIMEOUT);
    con.setRequestProperty("Accept", APPLICATION_JSON);
    con.setRequestProperty("Content-Type", APPLICATION_ZIP);
    con.setDoOutput(true);
    con.setFixedLengthStreamingMode(zipBytes.length);

    try {
      try (OutputStream os = con.getOutputStream()) {
        os.write(zipBytes);
      }
      int code = con.getResponseCode();
      String body = readBody(con, code);
      if (code != 200) {
        throw new IOException("HTTP " + code + " for POST " + uri + " body=" + body);
      }
      return JsonUtils.fromJson(body, StatisticsImportResponse.class);
    } finally {
      con.disconnect();
    }
  }

  // ============================================================================
  // Internal HTTP Methods
  // ============================================================================

  private void validateShortCode(String shortCode) {
    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null or blank");
    }
  }

  private <T> T executeGet(String path, Class<T> responseType)
      throws IOException {
    URI uri = serverBaseAdmin.resolve(path);
    logger().info("GET {}", uri);

    HttpURLConnection con = openConnection(uri, "GET");
    try {
      int code = con.getResponseCode();
      String body = readResponse(con, code);
      logger().info("GET - responseBody {}", body);
      if (code == 200) {
        var fromJson = JsonUtils.fromJson(body, responseType);
        logger().info("GET - from JSON {}", fromJson);
        return fromJson;
      }

      throw new IOException("HTTP " + code + " for GET " + uri + ": " + body);
    } finally {
      con.disconnect();
    }
  }

  private <T> Optional<T> executeGetOptional(String path, Class<T> responseType)
      throws IOException {
    URI uri = serverBaseAdmin.resolve(path);
    logger().info("executeGetOptional - GET {}", uri);

    HttpURLConnection con = openConnection(uri, "GET");
    try {
      int code = con.getResponseCode();
      String body = readResponse(con, code);
      logger().info("executeGetOptional - body {}", body);
      if (code == 200) {
        logger().info("executeGetOptional - GET 200");
        var fromJson = JsonUtils.fromJson(body, responseType);
        logger().info("executeGetOptional - GET - from Json {}", fromJson);
        return Optional.of(fromJson);
      }
      if (code == 404) {
        logger().info("executeGetOptional - GET 404");
        return Optional.empty();
      }

      throw new IOException("HTTP " + code + " for GET " + uri + ": " + body);
    } finally {
      con.disconnect();
    }
  }

  private <T> T executePut(String path, String jsonBody, Class<T> responseType)
      throws IOException {
    URI uri = serverBaseAdmin.resolve(path);
    logger().info("PUT {} body={}", uri, jsonBody);

    HttpURLConnection con = openConnection(uri, "PUT");
    con.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
    con.setDoOutput(true);

    try {
      byte[] payload = jsonBody.getBytes(UTF_8);
      con.setFixedLengthStreamingMode(payload.length);
      try (OutputStream os = con.getOutputStream()) {
        os.write(payload);
      }

      int code = con.getResponseCode();
      String body = readResponse(con, code);

      if (code == 200) {
        return JsonUtils.fromJson(body, responseType);
      }

      throw new IOException("HTTP " + code + " for PUT " + uri + ": " + body);
    } finally {
      con.disconnect();
    }
  }

  private HttpURLConnection openConnection(URI uri, String method)
      throws IOException {
    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
    con.setRequestMethod(method);
    con.setConnectTimeout(CONNECT_TIMEOUT);
    con.setReadTimeout(READ_TIMEOUT);
    con.setRequestProperty("Accept", APPLICATION_JSON);
    return con;
  }

  private String readResponse(HttpURLConnection con, int code)
      throws IOException {
    InputStream is = (code >= 400) ? con.getErrorStream() : con.getInputStream();
    if (is == null) {
      return "";
    }
    try (is) {
      return new String(is.readAllBytes(), UTF_8);
    }
  }

  // ============================================================================
  // Export/Import Helpers
  // ============================================================================

  private String buildExportPath(LocalDate from,
                                 LocalDate to,
                                 Collection<String> shortCodes,
                                 String exportTsIso) {
    StringBuilder qs = new StringBuilder();
    if (from != null) appendParam(qs, "from", from.toString());
    if (to != null) appendParam(qs, "to", to.toString());
    if (shortCodes != null && !shortCodes.isEmpty()) {
      String joined = String.join(",", shortCodes);
      appendParam(qs, "shortCodes", joined);
    }
    if (exportTsIso != null && !exportTsIso.isBlank()) {
      appendParam(qs, EXPORT_TIMESTAMP_PARAM, exportTsIso);
    }
    return qs.isEmpty()
        ? PATH_ADMIN_STATISTICS_EXPORT
        : PATH_ADMIN_STATISTICS_EXPORT + "?" + qs;
  }

  private static void appendParam(StringBuilder qs, String key, String value) {
    if (!qs.isEmpty()) qs.append('&');
    qs.append(key).append('=').append(URLEncoder.encode(value, UTF_8));
  }

  private InputStream fetchZipStream(String relativePath)
      throws IOException {
    URI uri = serverBaseAdmin.resolve(relativePath);
    HttpURLConnection con = openConnection(uri, "GET", APPLICATION_ZIP, null);
    int code;
    try {
      code = con.getResponseCode();
    } catch (IOException e) {
      con.disconnect();
      throw e;
    }
    if (code != 200) {
      try {
        String err = readErrorBody(con, code);
        throw new IOException("HTTP " + code + " for GET " + uri + " body=" + err);
      } finally {
        con.disconnect();
      }
    }
    return new ConnectionInputStream(con.getInputStream(), con);
  }

  private HeadExportInfo resolveExportFilenameViaHead(String relativePath)
      throws IOException {
    URI uri = serverBaseAdmin.resolve(relativePath);
    HttpURLConnection con = openConnection(uri, "HEAD", APPLICATION_ZIP, null);
    try {
      int code = con.getResponseCode();
      if (code != 200) {
        String err = readErrorBody(con, code);
        throw new IOException("HTTP " + code + " for HEAD " + uri + " body=" + err);
      }
      String ts = con.getHeaderField(EXPORT_TIMESTAMP_HEADER);
      if (ts == null || ts.isBlank()) ts = Instant.now().toString();
      String fallback = STATISTICS_EXPORT_FILE_NAME + ".zip";
      String filename = sanitizeFilename(extractFilenameFromContentDisposition(
          con.getHeaderField(CONTENT_DISPOSITION), fallback), fallback);
      return new HeadExportInfo(filename, ts);
    } finally {
      con.disconnect();
    }
  }

  private HttpURLConnection openConnection(URI uri, String method, String accept, String contentType)
      throws IOException {
    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
    con.setRequestMethod(method);
    con.setConnectTimeout(CONNECT_TIMEOUT);
    con.setReadTimeout(READ_TIMEOUT);
    if (accept != null) con.setRequestProperty("Accept", accept);
    if (contentType != null) con.setRequestProperty("Content-Type", contentType);
    return con;
  }

  private static String readBody(HttpURLConnection con, int code)
      throws IOException {
    InputStream is = (code >= 400) ? con.getErrorStream() : con.getInputStream();
    if (is == null) return "";
    try (is) {
      return new String(is.readAllBytes(), UTF_8);
    }
  }

  private static String readErrorBody(HttpURLConnection con, int code) {
    try {
      InputStream is = (con.getErrorStream() != null) ? con.getErrorStream() : con.getInputStream();
      if (is == null) return "";
      try (is) {
        return new String(is.readAllBytes(), UTF_8);
      }
    } catch (IOException e) {
      return "<error body unavailable: " + e.getMessage() + ">";
    }
  }

  private static String extractFilenameFromContentDisposition(String contentDisposition, String fallback) {
    if (contentDisposition == null || contentDisposition.isBlank()) return fallback;
    int idx = contentDisposition.toLowerCase(Locale.ROOT).indexOf("filename=");
    if (idx < 0) return fallback;
    String v = contentDisposition.substring(idx + "filename=".length()).trim();
    int semi = v.indexOf(';');
    if (semi >= 0) v = v.substring(0, semi).trim();
    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
      v = v.substring(1, v.length() - 1);
    }
    return v.isBlank() ? fallback : v;
  }

  private static String sanitizeFilename(String filename, String fallback) {
    if (filename == null || filename.isBlank()) return fallback;
    filename = filename.replace('\\', '/');
    int slash = filename.lastIndexOf('/');
    if (slash >= 0) filename = filename.substring(slash + 1);
    filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
    if (!filename.toLowerCase(Locale.ROOT).endsWith(".zip")) filename = filename + ".zip";
    return filename;
  }

  private record HeadExportInfo(String filename, String exportTsIso) { }

  public record ExportZipDownload(String filename, Supplier<InputStream> inputStreamFactory) { }
}
