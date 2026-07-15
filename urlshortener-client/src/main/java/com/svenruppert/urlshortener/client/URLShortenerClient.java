package com.svenruppert.urlshortener.client;


import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.AliasPolicy;
import com.svenruppert.urlshortener.core.JacksonJson;
import com.svenruppert.urlshortener.core.JsonUtils;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.core.urlmapping.BulkShortenRequest;
import com.svenruppert.urlshortener.core.urlmapping.BulkShortenResponse;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateRequest;
import com.svenruppert.urlshortener.core.urlmapping.BulkValidateResponse;
import com.svenruppert.urlshortener.core.urlmapping.ShortenRequest;
import com.svenruppert.urlshortener.core.urlmapping.ToggleActive.ToggleActiveRequest;
import com.svenruppert.urlshortener.core.urlmapping.UrlMappingListRequest;
import com.svenruppert.urlshortener.core.urlmapping.imports.ImportResult;
import com.svenruppert.urlshortener.core.validation.UrlValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.svenruppert.dependencies.core.net.HttpStatus.OK;
import static com.svenruppert.urlshortener.core.DefaultValues.*;
import static com.svenruppert.urlshortener.core.JsonUtils.fromJson;
import static com.svenruppert.urlshortener.core.JsonUtils.toJson;
import static java.nio.charset.StandardCharsets.UTF_8;

public class URLShortenerClient implements HasLogger {

  protected static final int CONNECT_TIMEOUT = 10_000;
  protected static final int READ_TIMEOUT = 15_000;

  private final URI serverBaseAdmin;
  private final URI serverBaseRedirect;

  public URLShortenerClient(String serverBaseUrlAdmin,
                            String serverBaseUrlRedirect) {
    var urlToServerAdmin = serverBaseUrlAdmin.endsWith("/")
        ? serverBaseUrlAdmin
        : serverBaseUrlAdmin + "/";

    var urlToServerRedirect = serverBaseUrlRedirect.endsWith("/")
        ? serverBaseUrlRedirect
        : serverBaseUrlRedirect + "/";

    logger().info("urlToServer ADMIN - {}", urlToServerAdmin);
    logger().info("urlToServer REDIRECT - {}", urlToServerRedirect);

    this.serverBaseAdmin = URI.create(urlToServerAdmin);
    this.serverBaseRedirect = URI.create(urlToServerRedirect);
  }

  public URLShortenerClient() {
    String adminUrl    = System.getenv().getOrDefault("ADMIN_SERVER_URL", ADMIN_SERVER_URL);
    String redirectUrl = System.getenv().getOrDefault("REDIRECT_SERVER_URL", DEFAULT_SERVER_URL);
    this(adminUrl, redirectUrl);
  }

  private static String readAllAsString(InputStream is) throws IOException {
    if (is == null) return "";
    return new String(is.readAllBytes(), UTF_8);
  }

  private static void drainQuietly(InputStream is) {
    if (is == null) return;
    try (is) {
      while (is.read() != -1) { /* discard */ }
    } catch (IOException ignored) {
    }
  }

  // ————————————————————————————————————————————————————————————————————————————
  // HTTP-Helper
  // ————————————————————————————————————————————————————————————————————————————

  private static String extractFilenameFromContentDisposition(String contentDisposition,
                                                              String fallback) {
    if (contentDisposition == null || contentDisposition.isBlank()) {
      return fallback;
    }

    // RFC 5987: filename*=UTF-8''...
    final String cd = contentDisposition;

    // 1) filename*=
    int idxStar = indexOfIgnoreCase(cd, "filename*=");
    if (idxStar >= 0) {
      String v = cd.substring(idxStar + "filename*=".length()).trim();
      v = stripParamValue(v);
      int utf8 = v.toLowerCase(Locale.ROOT).indexOf("utf-8''");
      if (utf8 >= 0) {
        String encoded = v.substring(utf8 + "utf-8''".length());
        try {
          return URLDecoder.decode(encoded, UTF_8);
        } catch (IllegalArgumentException ex) {
          // fall through
        }
      }
      return v;
    }

    // 2) filename=
    int idx = indexOfIgnoreCase(cd, "filename=");
    if (idx >= 0) {
      String v = cd.substring(idx + "filename=".length()).trim();
      v = stripParamValue(v);
      return v;
    }

    return fallback;
  }

  private static int indexOfIgnoreCase(String s, String needle) {
    return s.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
  }

  private static String stripParamValue(String v) {
    int semicolon = v.indexOf(';');
    if (semicolon >= 0) {
      v = v.substring(0, semicolon).trim();
    }
    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
      v = v.substring(1, v.length() - 1);
    }
    return v.trim();
  }

  private static String sanitizeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return EXPORT_DEFAULT_ZIP_FILENAME;
    }

    filename = filename.replace('\\', '/');
    int slash = filename.lastIndexOf('/');
    if (slash >= 0) {
      filename = filename.substring(slash + 1);
    }

    filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");

    if (!filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
      filename = filename + ".zip";
    }
    return filename;
  }

  private static String extractRelativePath(UrlMappingListRequest request) {
    final String qs = (request == null) ? "" : request.toQueryStringForExport();
    return qs.isEmpty()
        ? PATH_ADMIN_EXPORT
        : PATH_ADMIN_EXPORT + "?" + qs;
  }

  private static void writeBytes(HttpURLConnection con, byte[] payload) throws IOException {
    con.setDoOutput(true);
    con.setFixedLengthStreamingMode(payload.length);
    try (OutputStream os = con.getOutputStream()) {
      os.write(payload);
    }
  }

  private static byte[] readResponseBytes(HttpURLConnection con, int responseCode) throws IOException {
    final InputStream is =
        (responseCode >= 400)
            ? con.getErrorStream()
            : con.getInputStream();

    if (is == null) {
      return new byte[0];
    }

    try (InputStream in = is) {
      return in.readAllBytes();
    }
  }

  private static void requireStatus(HttpURLConnection con, int expected) throws IOException {
    int code = con.getResponseCode();
    if (code != expected) {
      byte[] body = readResponseBytes(con, code);
      String snippet = new String(body, java.nio.charset.StandardCharsets.UTF_8);
      if (snippet.length() > 2_000) snippet = snippet.substring(0, 2_000) + "...";
      throw new IOException("HTTP " + code + " expected " + expected + " for " + con.getURL()
                                + " response: " + snippet);
    }
  }

  private static boolean is2xx(int code) {
    return code >= 200 && code < 300;
  }

  private Response postJson(URI uri, String jsonBody) throws IOException {
    HttpURLConnection con = openConnection(uri, "POST", APPLICATION_JSON, JSON_CONTENT_TYPE);
    try {
      writeBytes(con, jsonBody.getBytes(UTF_8));
      int code = con.getResponseCode();
      String body = new String(readResponseBytes(con, code), UTF_8);
      return new Response(code, body);
    } finally {
      con.disconnect();
    }
  }

  private String postShortenRaw(String jsonBody) throws IOException {
    URI uri = serverBaseAdmin.resolve(PATH_ADMIN_SHORTEN);
    HttpURLConnection con = openConnection(uri, "POST", APPLICATION_JSON, JSON_CONTENT_TYPE);
    try {
      writeBytes(con, jsonBody.getBytes(UTF_8));
      requireStatusOneOf(con, 200, 201);
      return new String(readResponseBytes(con, con.getResponseCode()), UTF_8);
    } finally {
      con.disconnect();
    }
  }

  /**
   * Creates a short URL for the given original URL using the default alias policy.
   */
  private String shortenURL(String originalUrl) throws IOException {

    var validation = UrlValidator.validate(originalUrl);
    if (!validation.valid()) {
      throw new IllegalArgumentException("Invalid URL: " + validation.message());
    }

    logger().info("shortenURL - valid URL {}", originalUrl);

    // NOTE: ShortenRequest now uses "shortURL" in JSON contract (no alias).
    final String body = toJson(new ShortenRequest(originalUrl, null, null, null));
    logger().info("shortenURL - body - '{}'", body);

    final String jsonResponse = postShortenRaw(body);
    final String extractedShortCode = JsonUtils.extractShortCode(jsonResponse);

    logger().info("extractedShortCode .. {}", extractedShortCode);
    return extractedShortCode;
  }

  public String resolveShortcode(String shortCode) throws IOException {
    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null/blank");
    }
    logger().info("Resolving shortCode: {}", shortCode);
    final URI uri = serverBaseRedirect.resolve(PATH_REDIRECT + shortCode);
    logger().info("resolveShortcode - url .. {}", uri);
    final HttpURLConnection con = openConnection(uri, "GET", "*/*", null);
    con.setInstanceFollowRedirects(false);
    try {
      final int code = con.getResponseCode();
      logger().info("resolveShortcode - responseCode .. {}", code);
      if (code == 301 || code == 302) {
        final String location = con.getHeaderField("Location");
        logger().info("location .. {}", location);
        drainQuietly(con.getInputStream());
        return location;
      }
      if (code == 404) {
        drainQuietly(con.getErrorStream());
        return null;
      }
      final String body = readAllAsString(
          con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream());
      throw new IOException("Unexpected HTTP " + code + " for GET " + uri + " body=" + body);
    } finally {
      con.disconnect();
    }
  }

  public int listCount(UrlMappingListRequest req) throws IOException {
    logger().info("listCount - request {}", req);
    String qs = (req == null) ? "" : req.toQueryStringForCount();
    URI uri = qs.isEmpty()
        ? serverBaseAdmin.resolve(PATH_ADMIN_LIST_COUNT)
        : serverBaseAdmin.resolve(PATH_ADMIN_LIST_COUNT + "?" + qs);

    String body = requestJson(uri, "GET", null, OK.code());

    JsonNode root = JacksonJson.mapper().readTree(body);
    logger().info("listCount - root {}", root);
    JsonNode total = root.get("total");
    logger().info("listCount - totalCount {}", total);
    return (total == null || total.isNull()) ? 0 : total.asInt(0);
  }

  public List<ShortUrlMapping> listAll() throws IOException {
    final String json = fetchJson(PATH_ADMIN_LIST_ALL);
    logger().info("listAll - json {}", json);
    return parseItemsAsMappings(json);
  }

  public List<ShortUrlMapping> listExpired() throws IOException {
    final String json = fetchJson(PATH_ADMIN_LIST_EXPIRED);
    return parseItemsAsMappings(json);
  }

  public List<ShortUrlMapping> listActive() throws IOException {
    final String json = fetchJson(PATH_ADMIN_LIST_ACTIVE);
    return parseItemsAsMappings(json);
  }

  public List<ShortUrlMapping> listInActive() throws IOException {
    final String json = fetchJson(PATH_ADMIN_LIST_INACTIVE);
    return parseItemsAsMappings(json);
  }

  public List<ShortUrlMapping> list(UrlMappingListRequest request) throws IOException {
    logger().info("list - UrlMappingListRequest: {}", request);
    final String json = listAsJson(request);
    logger().info("list - UrlMappingListRequest - json: {}", json);
    return parseItemsAsMappings(json);
  }

  /**
   * Gets a single mapping by its exact shortCode.
   *
   * @param shortCode the short code to look up
   * @return Optional containing the mapping if found, empty otherwise
   * @throws IOException if the request fails
   * @throws IllegalArgumentException if shortCode is null or blank
   */
  public java.util.Optional<ShortUrlMapping> getMapping(String shortCode) throws IOException {
    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null/blank");
    }
    logger().info("getMapping - shortCode: {}", shortCode);

    // Use list with exact codePart filter
    UrlMappingListRequest request = UrlMappingListRequest.builder()
        .codePart(shortCode)
        .page(1)
        .size(10)
        .build();

    List<ShortUrlMapping> results = list(request);

    // Find exact match (codePart is a contains filter, so we need exact match)
    return results.stream()
        .filter(m -> shortCode.equals(m.shortCode()))
        .findFirst();
  }

  public String importValidateRaw(byte[] zipBytes) throws IOException {
    if (zipBytes == null || zipBytes.length == 0) {
      throw new IllegalArgumentException("zipBytes is null/empty");
    }
    URI uri = serverBaseAdmin.resolve(PATH_ADMIN_IMPORT_VALIDATE);
    return postZipExpectJson(uri, zipBytes, OK.code());
  }

  public ImportResult importApply(String stagingId) throws IOException {
    if (stagingId == null || stagingId.isBlank()) {
      throw new IllegalArgumentException("stagingId must not be null/blank");
    }

    String relativePath = PATH_ADMIN_IMPORT_APPLY
        + "?stagingId=" + URLEncoder.encode(stagingId, UTF_8);
    URI uri = serverBaseAdmin.resolve(relativePath);

    String body = requestJson(uri, "POST", null, OK.code());
    logger().info("importApply - body {}", body);
    return fromJson(body, ImportResult.class);
  }

  public InputStream exportAllAsZipStream(UrlMappingListRequest request) throws IOException {
    final var getPath = getExportPath(request);
    return fetchZipStream(getPath);
  }

  private String getExportPath(UrlMappingListRequest request) throws IOException {
    final var relativePath = extractRelativePath(request);
    final HeadExportInfo info = resolveExportFilenameViaHead(relativePath);
    return appendExportTimestamp(relativePath, info.exportTsIso());
  }

  private String appendExportTimestamp(String relativePath, String exportTsIso) {
    final String encoded = URLEncoder.encode(exportTsIso, UTF_8);
    return relativePath.contains("?")
        ? relativePath + "&" + EXPORT_TIMESTAMP_PARAM + "=" + encoded
        : relativePath + "?" + EXPORT_TIMESTAMP_PARAM + "=" + encoded;
  }

  public ExportZipDownload exportAllAsZipDownload(UrlMappingListRequest request) throws IOException {

    final var relativePath = extractRelativePath(request);
    final HeadExportInfo info = resolveExportFilenameViaHead(relativePath);
    final String getPath = appendExportTimestamp(relativePath, info.exportTsIso());

    final Supplier<InputStream> factory = () -> {
      try {
        return fetchZipStream(getPath);
      } catch (IOException e) {
        throw new RuntimeException("Failed to open export ZIP stream", e);
      }
    };
    return new ExportZipDownload(info.filename(), factory);
  }

  private HeadExportInfo resolveExportFilenameViaHead(String relativePath) throws IOException {

    final URI uri = serverBaseAdmin.resolve(relativePath);
    final URL url = uri.toURL();
    logger().info("resolveExportFilenameViaHead - url {}", url);

    final HttpURLConnection con = openConnection(uri, "HEAD", APPLICATION_ZIP, null);
    try {
      int code = con.getResponseCode();
      if (code != OK.code()) {
        String err = readAllAsString(con.getErrorStream());
        throw new IOException("Unexpected HTTP " + code + " for HEAD " + url + " body=" + err);
      }
      String exportTs = con.getHeaderField(EXPORT_TIMESTAMP_HEADER);
      if (exportTs == null || exportTs.isBlank()) exportTs = Instant.now().toString();

      final String filename = sanitizeFilename(extractFilenameFromContentDisposition(
          con.getHeaderField("Content-Disposition"),
          EXPORT_DEFAULT_ZIP_FILENAME));

      return new HeadExportInfo(filename, exportTs);

    } finally {
      con.disconnect();
    }
  }

  public Path exportAllAsZipFile(UrlMappingListRequest request, Path targetDirectory) throws IOException {

    final String qs = (request == null) ? "" : request.toQueryStringForExport();
    final String relativePath = qs.isBlank()
        ? PATH_ADMIN_EXPORT
        : PATH_ADMIN_EXPORT + "?" + qs;

    final URI uri = serverBaseAdmin.resolve(relativePath);
    final URL url = uri.toURL();
    logger().info("exportAllAsZipFile - {}", url);

    final HttpURLConnection con = openConnection(uri, "GET", APPLICATION_ZIP, null);
    try {
      final int code = con.getResponseCode();
      if (code != OK.code()) {
        final InputStream es = con.getErrorStream();
        final String err = readAllAsString(es);
        throw new IOException("Unexpected HTTP " + code + " for GET " + url + " body=" + err);
      }

      Files.createDirectories(targetDirectory);

      final String fallbackName = EXPORT_DEFAULT_ZIP_FILENAME;
      final String filename = sanitizeFilename(extractFilenameFromContentDisposition(
          con.getHeaderField(CONTENT_DISPOSITION),
          fallbackName));

      final Path normalizedDir = targetDirectory.normalize();
      final Path target = normalizedDir.resolve(filename).normalize();

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

  public String listAllAsJson() throws IOException {
    return fetchJson(PATH_ADMIN_LIST_ALL);
  }

  public String listExpiredAsJson() throws IOException {
    return fetchJson(PATH_ADMIN_LIST_EXPIRED);
  }

  public String listActiveAsJson() throws IOException {
    return fetchJson(PATH_ADMIN_LIST_ACTIVE);
  }

  public String listAsJson(UrlMappingListRequest request) throws IOException {
    final String qs = (request == null) ? "" : request.toQueryString();
    String relativePath = qs.isEmpty()
        ? PATH_ADMIN_LIST
        : PATH_ADMIN_LIST + "?" + qs;
    return fetchJson(relativePath);
  }

  private String fetchJson(String relativePath) throws IOException {
    final URI uri = serverBaseAdmin.resolve(relativePath);
    logger().info("fetchJson - url {}", uri);
    return requestJson(uri, "GET", null, OK.code());
  }

  private InputStream fetchZipStream(String relativePath) throws IOException {

    final URI uri = serverBaseAdmin.resolve(relativePath);
    final URL url = uri.toURL();
    logger().info("fetchZipStream - url {}", url);

    final HttpURLConnection con = openConnection(uri, "GET", APPLICATION_ZIP, null);

    final int code;
    try {
      code = con.getResponseCode();
    } catch (IOException e) {
      con.disconnect();
      throw e;
    }

    if (code != OK.code()) {
      try {
        final InputStream es = (con.getErrorStream() != null) ? con.getErrorStream() : con.getInputStream();
        final String err = readAllAsString(es);
        throw new IOException("Unexpected HTTP " + code + " for GET " + url + " body=" + err);
      } finally {
        con.disconnect();
      }
    }

    final InputStream raw = con.getInputStream();
    return new ConnectionInputStream(raw, con);
  }

  private List<ShortUrlMapping> parseItemsAsMappings(String json) throws IOException {
    if (json == null || json.isBlank()) return List.of();

    final ObjectMapper mapper = JacksonJson.mapper();
    final JsonNode root = mapper.readTree(json);

    final JsonNode items = root.get("items");
    logger().info("parseItemsAsMappings JsonNode items {}", items);
    if (items == null || !items.isArray() || items.isEmpty()) {
      return List.of();
    }

    final List<ShortUrlMapping> result = new ArrayList<>(items.size());
    for (JsonNode item : items) {
      result.add(mapper.treeToValue(item, ShortUrlMapping.class));
    }
    return result;
  }

  private String postZipExpectJson(URI uri, byte[] zipBytes, int expectedStatus) throws IOException {
    HttpURLConnection con = openConnection(uri, "POST", APPLICATION_JSON, APPLICATION_ZIP);
    con.setInstanceFollowRedirects(false);
    try {
      writeBytes(con, zipBytes);
      requireStatus(con, expectedStatus);
      return new String(readResponseBytes(con, con.getResponseCode()), UTF_8);
    } finally {
      con.disconnect();
    }
  }

  /**
   * Creates short links for the given URLs in a single server round-trip.
   *
   * @param urls non-null, non-empty list of target URLs (max {@value BulkShortenRequest#MAX_URLS})
   * @return server response with per-item results, counters, and the full short URL for every
   *         successfully created link
   * @throws IllegalArgumentException if {@code urls} is null, empty, or the server rejects the
   *                                  request with 400
   * @throws IOException              on network or unexpected HTTP errors
   */
  public BulkShortenResponse bulkShorten(List<String> urls) throws IOException {
    return bulkShorten(urls, null, null);
  }

  /**
   * Creates short links for the given URLs using optional shared defaults.
   *
   * @param urls             non-null, non-empty list of target URLs
   * @param defaultExpiresAt optional expiry timestamp applied to every link; {@code null} = no expiry
   * @param defaultActive    optional active flag for every link; {@code null} defaults to {@code true}
   */
  public BulkShortenResponse bulkShorten(List<String> urls,
                                         java.time.Instant defaultExpiresAt,
                                         Boolean defaultActive) throws IOException {
    if (urls == null || urls.isEmpty()) {
      throw new IllegalArgumentException("urls must not be null or empty");
    }
    logger().info("bulkShorten - {} URLs, expiresAt={}, active={}", urls.size(), defaultExpiresAt, defaultActive);
    final String jsonBody = toJson(new BulkShortenRequest(urls, defaultExpiresAt, defaultActive));
    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_SHORTEN_BULK);
    final Response resp = postJson(uri, jsonBody);
    logger().info("bulkShorten - response code {}", resp.code());
    if (resp.code() == 200) {
      return fromJson(resp.body(), BulkShortenResponse.class);
    }
    if (resp.code() == 400) {
      throw new IllegalArgumentException("Bad request: " + resp.body());
    }
    throw new IOException("Unexpected HTTP " + resp.code() + " for POST " + uri + " body=" + resp.body());
  }

  /**
   * Validates a batch of URLs without creating any shortlinks.
   *
   * @param newUrls      non-null, non-empty list of URLs to validate
   * @param existingUrls URLs currently in the UI work set (for DUPLICATE_IN_GRID detection);
   *                     may be {@code null} or empty
   * @return validation response with per-item results
   * @throws IllegalArgumentException if {@code newUrls} is null/empty or the server returns 400
   * @throws IOException              on network or unexpected HTTP errors
   */
  public BulkValidateResponse bulkValidate(List<String> newUrls,
                                           List<String> existingUrls) throws IOException {
    if (newUrls == null || newUrls.isEmpty()) {
      throw new IllegalArgumentException("newUrls must not be null or empty");
    }
    logger().info("bulkValidate - {} new URLs, {} existing",
        newUrls.size(), existingUrls != null ? existingUrls.size() : 0);
    final String jsonBody = toJson(
        new BulkValidateRequest(newUrls, existingUrls != null ? existingUrls : List.of()));
    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_VALIDATE_BULK);
    final Response resp = postJson(uri, jsonBody);
    logger().info("bulkValidate - response code {}", resp.code());
    if (resp.code() == 200) {
      return fromJson(resp.body(), BulkValidateResponse.class);
    }
    if (resp.code() == 400) {
      throw new IllegalArgumentException("Bad request: " + resp.body());
    }
    throw new IOException(
        "Unexpected HTTP " + resp.code() + " for POST " + uri + " body=" + resp.body());
  }

  public ShortUrlMapping createMapping(String url) throws IOException {
    logger().info("Create mapping url='{}'", url);
    var shortenURL = shortenURL(url);
    return new ShortUrlMapping(shortenURL, url, Instant.now(), null, true);
  }

  public ShortUrlMapping createCustomMapping(String shortURL, String url) throws IOException {
    logger().info("Create custom mapping shortURL='{}' url='{}'", shortURL, url);
    return createCustomMapping(shortURL, url, null, null);
  }

  public ShortUrlMapping createCustomMapping(String shortURL,
                                             String url,
                                             Instant expiredAtOrNull,
                                             Boolean activeOrNull) throws IOException {

    logger().info("Create custom mapping shortURL='{}' url='{}' expiredAt='{}' active='{}'",
                  shortURL, url, expiredAtOrNull, activeOrNull);

    var urlValidation = UrlValidator.validate(url);
    if (!urlValidation.valid()) {
      throw new IllegalArgumentException("Invalid URL: " + urlValidation.message());
    }

    if (shortURL != null && !shortURL.isBlank()) {
      var aliasValidation = AliasPolicy.validate(shortURL);
      if (!aliasValidation.valid()) {
        var reason = aliasValidation.reason();
        throw new IllegalArgumentException(reason.defaultMessage);
      }
    }

    final ShortenRequest shortenRequest =
        new ShortenRequest(url, shortURL, expiredAtOrNull, activeOrNull);

    final String jsonBody = toJson(shortenRequest);
    logger().info("createCustomMapping - body - '{}'", jsonBody);

    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_SHORTEN);
    final Response resp = postJson(uri, jsonBody);

    logger().info("Response Code from Server - {}", resp.code());

    if (resp.code() == 200 || resp.code() == 201) {
      logger().info("createCustomMapping - jsonResponse - {}", resp.body());
      final ShortUrlMapping shortUrlMapping = fromJson(resp.body(), ShortUrlMapping.class);
      logger().info("shortUrlMapping .. {}", shortUrlMapping);
      return shortUrlMapping;
    }

    if (resp.code() == 409) {
      throw new IllegalArgumentException("shortURL already in use: '" + shortURL + "'. " + resp.body());
    }

    if (resp.code() == 400) {
      throw new IllegalArgumentException("Bad request: " + resp.body());
    }

    throw new IOException("Unexpected HTTP " + resp.code() + " for POST " + uri + " body=" + resp.body());
  }

  private boolean putJsonReturnBooleanOn404(URI uri, String jsonBody) throws IOException {

    final HttpURLConnection con = openConnection(uri, "PUT", APPLICATION_JSON, JSON_CONTENT_TYPE);
    try {
      writeBytes(con, jsonBody.getBytes(UTF_8));
      final int code = con.getResponseCode();
      if (code == 404) {
        drainQuietly(con.getErrorStream());
        return false;
      }
      if (!is2xx(code)) {
        final String err = readAllAsString(
            con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream());
        throw new IOException("Unexpected HTTP " + code + " for PUT " + uri + " body=" + err);
      }
      drainQuietly(con.getInputStream());
      return true;
    } finally {
      con.disconnect();
    }
  }

  public boolean edit(String shortCode,
                      String newUrl,
                      Instant expiresAtOrNull,
                      Boolean activeOrNull) throws IOException {

    logger().info("Edit mapping shortCode='{}' url='{}' expiredAt='{}' active='{}'",
                  shortCode, newUrl, expiresAtOrNull, activeOrNull);

    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null/blank");
    }
    if (newUrl == null || newUrl.isBlank()) {
      throw new IllegalArgumentException("newUrl must not be null/blank");
    }

    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_EDIT + "/" + shortCode);
    logger().info("edit - {}", uri);

    final ShortenRequest req =
        new ShortenRequest(newUrl, shortCode, expiresAtOrNull, activeOrNull);
    final String body = toJson(req);
    logger().info("edit - request body - '{}'", body);

    return putJsonReturnBooleanOn404(uri, body);
  }

  public boolean delete(String shortCode) throws IOException {

    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null/blank");
    }

    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_DELETE + "/" + shortCode);
    logger().info("delete - {}", uri);

    final HttpURLConnection con = openConnection(uri, "DELETE", APPLICATION_JSON, null);
    try {
      final int code = con.getResponseCode();

      if (code == 404) {
        drainQuietly(con.getErrorStream());
        return false;
      }

      if (!is2xx(code)) {
        final String body = readAllAsString(
            con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream());
        throw new IOException("Unexpected HTTP " + code + " for DELETE " + uri + " body=" + body);
      }

      drainQuietly(con.getInputStream());
      return true;

    } finally {
      con.disconnect();
    }
  }

  public void deleteOrThrow(String shortCode) throws IOException {
    boolean removed = delete(shortCode);
    if (!removed) {
      throw new IOException("shortCode not found: " + shortCode);
    }
  }

  public boolean toggleActive(String shortCode, boolean active) throws IOException {

    logger().info("Toggle Active shortCode='{}' active='{}'", shortCode, active);

    if (shortCode == null || shortCode.isBlank()) {
      throw new IllegalArgumentException("shortCode must not be null/blank");
    }

    final URI uri = serverBaseAdmin.resolve(PATH_ADMIN_TOGGLE_ACTIVE + "/" + shortCode);
    logger().info("Toggle Active - {}", uri);

    final ToggleActiveRequest req = new ToggleActiveRequest(shortCode, active);
    final String body = toJson(req);
    logger().info("Toggle Active - request body - '{}'", body);

    return putJsonReturnBooleanOn404(uri, body);
  }

  private HttpURLConnection openConnection(URI uri,
                                           String method,
                                           String accept,
                                           String contentType) throws IOException {
    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
    con.setRequestMethod(method);

    con.setConnectTimeout(CONNECT_TIMEOUT);
    con.setReadTimeout(READ_TIMEOUT);

    if (accept != null && !accept.isBlank()) {
      con.setRequestProperty("Accept", accept);
    }
    if (contentType != null && !contentType.isBlank()) {
      con.setRequestProperty("Content-Type", contentType);
    }
    return con;
  }

  private String requestJson(URI uri,
                             String method,
                             String jsonBodyOrNull,
                             int expectedStatus) throws IOException {

    final HttpURLConnection con = openConnection(uri, method, APPLICATION_JSON, JSON_CONTENT_TYPE);
    try {
      if (jsonBodyOrNull != null) {
        writeBytes(con, jsonBodyOrNull.getBytes(UTF_8));
      }

      final int code = con.getResponseCode();
      if (code != expectedStatus) {
        final String err = readAllAsString(
            con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream());
        throw new IOException("Unexpected HTTP " + code + " for " + method + " " + uri + " body=" + err);
      }

      return new String(readResponseBytes(con, code), UTF_8);

    } finally {
      con.disconnect();
    }
  }

  private void requireStatusOneOf(HttpURLConnection con, int... allowed) throws IOException {
    int code = con.getResponseCode();
    for (int a : allowed) if (code == a) return;

    byte[] body = readResponseBytes(con, code);
    String snippet = new String(body, java.nio.charset.StandardCharsets.UTF_8);
    if (snippet.length() > 2_000) snippet = snippet.substring(0, 2_000) + "...";
    throw new IOException("HTTP " + code + " expected one of " + java.util.Arrays.toString(allowed)
                              + " for " + con.getURL() + " response: " + snippet);
  }

  private record Response(int code, String body) { }

  private record HeadExportInfo(String filename, String exportTsIso) { }

  public record ExportZipDownload(String filename, Supplier<InputStream> inputStreamFactory) { }
}
