package com.svenruppert.urlshortener.core.urlmapping;





import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for the bulk-validate endpoint.
 *
 * <p>No data is persisted during validation. Each entry in {@code results} carries
 * a machine-readable {@link ValidationStatus} that the UI uses to drive the
 * Validate/Create button state, status badges, and row-level actions.
 */
public final class BulkValidateResponse {

  private List<ValidationItemResult> results;

  public BulkValidateResponse() {
  }

  public BulkValidateResponse(List<ValidationItemResult> results) {
    this.results = results;
  }

  public List<ValidationItemResult> getResults() {
    return results;
  }

  public void setResults(List<ValidationItemResult> results) {
    this.results = results;
  }

  // ── Status enum ──────────────────────────────────────────────────────────────

  /**
   * Machine-readable outcome of a single validation entry.
   *
   * <ul>
   *   <li>{@code VALID}                 – passes all checks; no existing shortlinks found.</li>
   *   <li>{@code EMPTY}                 – input was blank. <b>Blocking.</b></li>
   *   <li>{@code INVALID_URL}           – format validation failed. <b>Blocking.</b></li>
   *   <li>{@code TOO_LONG}              – exceeds max URL length. <b>Blocking.</b></li>
   *   <li>{@code DUPLICATE_IN_BATCH}    – duplicate within the submitted batch. <b>Blocking.</b></li>
   *   <li>{@code DUPLICATE_IN_GRID}     – already present in the UI work set. <b>Blocking.</b></li>
   *   <li>{@code HAS_EXISTING_SHORTLINKS} – valid but existing shortlinks already target this URL.
   *       <em>Non-blocking warning</em>: the user may still create a new link.</li>
   * </ul>
   */
  public enum ValidationStatus {
    VALID,
    EMPTY,
    INVALID_URL,
    TOO_LONG,
    DUPLICATE_IN_BATCH,
    DUPLICATE_IN_GRID,
    HAS_EXISTING_SHORTLINKS;

    /** Returns {@code true} for statuses that prevent link creation. */
    @JsonIgnore
    public boolean isBlocking() {
      return this == EMPTY
          || this == INVALID_URL
          || this == TOO_LONG
          || this == DUPLICATE_IN_BATCH
          || this == DUPLICATE_IN_GRID;
    }

    /** Returns {@code true} for statuses that allow link creation to proceed. */
    @JsonIgnore
    public boolean isCreatable() {
      return this == VALID || this == HAS_EXISTING_SHORTLINKS;
    }
  }

  // ── Per-item result ──────────────────────────────────────────────────────────

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ValidationItemResult {

    /** 0-based position in the original input list. */
    private int index;
    private String originalUrl;
    /** Trimmed/normalised URL (equals originalUrl after stripping whitespace). */
    private String normalizedUrl;
    private ValidationStatus status;
    /** Shortcodes of existing mappings that already point to this URL. Empty when none. */
    private List<String> existingShortCodes;
    private List<ExistingShortlinkInfo> existingShortlinkInfos;
    private String errorMessage;

    public ValidationItemResult() {
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static ValidationItemResult valid(int index,
                                             String originalUrl,
                                             String normalizedUrl) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = normalizedUrl;
      r.status = ValidationStatus.VALID;
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult hasExisting(int index,
                                                   String originalUrl,
                                                   String normalizedUrl,
                                                   List<ExistingShortlinkInfo> existingInfos) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = normalizedUrl;
      r.status = ValidationStatus.HAS_EXISTING_SHORTLINKS;
      r.existingShortlinkInfos = List.copyOf(existingInfos);
      r.existingShortCodes = existingInfos.stream()
          .map(ExistingShortlinkInfo::getShortCode)
          .toList();
      return r;
    }

    public static ValidationItemResult empty(int index, String originalUrl) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl != null ? originalUrl : "";
      r.normalizedUrl = "";
      r.status = ValidationStatus.EMPTY;
      r.errorMessage = "URL must not be blank";
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult invalidUrl(int index,
                                                  String originalUrl,
                                                  String errorMessage) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = originalUrl;
      r.status = ValidationStatus.INVALID_URL;
      r.errorMessage = errorMessage;
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult tooLong(int index, String originalUrl) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = originalUrl;
      r.status = ValidationStatus.TOO_LONG;
      r.errorMessage = "URL exceeds maximum length of "
          + BulkValidateRequest.MAX_URL_LENGTH + " characters";
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult duplicateInBatch(int index, String originalUrl) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = originalUrl;
      r.status = ValidationStatus.DUPLICATE_IN_BATCH;
      r.errorMessage = "Duplicate URL within the submitted batch";
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult duplicateInGrid(int index, String originalUrl) {
      var r = new ValidationItemResult();
      r.index = index;
      r.originalUrl = originalUrl;
      r.normalizedUrl = originalUrl;
      r.status = ValidationStatus.DUPLICATE_IN_GRID;
      r.errorMessage = "URL already present in the work set";
      r.existingShortCodes = List.of();
      r.existingShortlinkInfos = List.of();
      return r;
    }

    public static ValidationItemResult duplicateInGrid(int index,
                                                       String originalUrl,
                                                       String customMessage) {
      var r = duplicateInGrid(index, originalUrl);
      r.errorMessage = customMessage;
      return r;
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** {@code true} if this entry may proceed to link creation. Excluded from JSON. */
    @JsonIgnore
    public boolean isCreatable() {
      return status != null && status.isCreatable();
    }

    /** {@code true} if this entry blocks link creation. Excluded from JSON. */
    @JsonIgnore
    public boolean isBlocking() {
      return status != null && status.isBlocking();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public String getOriginalUrl() {
      return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
      this.originalUrl = originalUrl;
    }

    public String getNormalizedUrl() {
      return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
      this.normalizedUrl = normalizedUrl;
    }

    public ValidationStatus getStatus() {
      return status;
    }

    public void setStatus(ValidationStatus status) {
      this.status = status;
    }

    public List<String> getExistingShortCodes() {
      return existingShortCodes;
    }

    public void setExistingShortCodes(List<String> existingShortCodes) {
      this.existingShortCodes = existingShortCodes;
    }

    public List<ExistingShortlinkInfo> getExistingShortlinkInfos() {
      return existingShortlinkInfos;
    }

    public void setExistingShortlinkInfos(List<ExistingShortlinkInfo> existingShortlinkInfos) {
      this.existingShortlinkInfos = existingShortlinkInfos;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }
  }

  // ── Enriched info about one existing shortlink ────────────────────────────────

  /**
   * Enriched information about an existing shortlink that already targets the validated URL.
   * <p>Also used to surface <em>protocol-variant</em> matches: when the store has a mapping for
   * {@code http://example.com} and the submitted URL is {@code https://example.com} (or vice-versa),
   * {@link #isProtocolVariant()} returns {@code true}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ExistingShortlinkInfo {

    private String shortCode;
    private boolean active;
    /** {@code null} means no expiry date. */
    private Instant expiresAt;
    /**
     * {@code true} when this shortlink targets the http↔https counterpart of the validated URL.
     */
    private boolean protocolVariant;

    public ExistingShortlinkInfo() {
    }

    public ExistingShortlinkInfo(String shortCode,
                                 boolean active,
                                 Instant expiresAt,
                                 boolean protocolVariant) {
      this.shortCode = shortCode;
      this.active = active;
      this.expiresAt = expiresAt;
      this.protocolVariant = protocolVariant;
    }

    public String getShortCode() {
      return shortCode;
    }

    public void setShortCode(String shortCode) {
      this.shortCode = shortCode;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    public Instant getExpiresAt() {
      return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
    }

    public boolean isProtocolVariant() {
      return protocolVariant;
    }

    public void setProtocolVariant(boolean protocolVariant) {
      this.protocolVariant = protocolVariant;
    }
  }
}