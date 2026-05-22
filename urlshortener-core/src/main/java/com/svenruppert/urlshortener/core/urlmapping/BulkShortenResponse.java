package com.svenruppert.urlshortener.core.urlmapping;





import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response DTO for the bulk short-link creation endpoint.
 *
 * <p>Top-level counters ({@code total}, {@code succeeded}, {@code failed}) give a quick
 * overview of the run.  The {@code results} list preserves the original input order so
 * that every item can be correlated back to its source line via {@code index}.
 */
public final class BulkShortenResponse {

  private List<BulkShortenItemResult> results;
  private int total;
  private int succeeded;
  private int failed;

  public BulkShortenResponse() {
  }

  public BulkShortenResponse(List<BulkShortenItemResult> results) {
    this.results   = results;
    this.total     = results.size();
    this.succeeded = (int) results.stream().filter(r -> r.getStatus() == ItemStatus.CREATED).count();
    this.failed    = this.total - this.succeeded;
  }

  // ── Top-level accessors ────────────────────────────────────────────────────

  public List<BulkShortenItemResult> getResults() {
    return results;
  }

  public void setResults(List<BulkShortenItemResult> results) {
    this.results = results;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getSucceeded() {
    return succeeded;
  }

  public void setSucceeded(int succeeded) {
    this.succeeded = succeeded;
  }

  public int getFailed() {
    return failed;
  }

  public void setFailed(int failed) {
    this.failed = failed;
  }

  // ── Status enum ───────────────────────────────────────────────────────────

  /**
   * Explicit, machine-readable outcome for a single bulk entry.
   *
   * <ul>
   *   <li>{@code CREATED}        – link was successfully created and is immediately active.</li>
   *   <li>{@code INVALID_URL}    – the supplied URL failed validation (bad scheme, missing TLD, …).</li>
   *   <li>{@code TOO_LONG}       – the URL exceeds the maximum allowed length.</li>
   *   <li>{@code LIMIT_EXCEEDED} – the batch exceeded the maximum number of allowed URLs.</li>
   *   <li>{@code FAILED}         – creation failed for an internal / unexpected reason.</li>
   * </ul>
   */
  public enum ItemStatus {
    CREATED,
    INVALID_URL,
    TOO_LONG,
    LIMIT_EXCEEDED,
    FAILED
  }

  // ── Per-item result ────────────────────────────────────────────────────────

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class BulkShortenItemResult {

    /** 0-based position in the original input list. */
    private int index;
    private String originalUrl;
    private String shortCode;
    /** Fully qualified short URL ready to copy/share (base URL + shortCode). */
    private String shortUrl;
    private ItemStatus status;
    private String errorMessage;

    public BulkShortenItemResult() {
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    public static BulkShortenItemResult success(int index,
                                                String originalUrl,
                                                String shortCode,
                                                String shortUrl) {
      var r = new BulkShortenItemResult();
      r.index       = index;
      r.originalUrl = originalUrl;
      r.shortCode   = shortCode;
      r.shortUrl    = shortUrl;
      r.status      = ItemStatus.CREATED;
      return r;
    }

    public static BulkShortenItemResult invalidUrl(int index,
                                                   String originalUrl,
                                                   String errorMessage) {
      var r = new BulkShortenItemResult();
      r.index        = index;
      r.originalUrl  = originalUrl;
      r.status       = ItemStatus.INVALID_URL;
      r.errorMessage = errorMessage;
      return r;
    }

    public static BulkShortenItemResult tooLong(int index, String originalUrl) {
      var r = new BulkShortenItemResult();
      r.index        = index;
      r.originalUrl  = originalUrl;
      r.status       = ItemStatus.TOO_LONG;
      r.errorMessage = "URL exceeds maximum length of "
          + BulkShortenRequest.MAX_URL_LENGTH + " characters";
      return r;
    }

    public static BulkShortenItemResult failed(int index,
                                               String originalUrl,
                                               String errorMessage) {
      var r = new BulkShortenItemResult();
      r.index        = index;
      r.originalUrl  = originalUrl;
      r.status       = ItemStatus.FAILED;
      r.errorMessage = errorMessage;
      return r;
    }

    // ── Convenience ─────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when status is {@link ItemStatus#CREATED}.
     * Excluded from JSON serialization — callers should evaluate {@code status} directly.
     */
    @JsonIgnore
    public boolean isSuccess() {
      return status == ItemStatus.CREATED;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

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

    public String getShortCode() {
      return shortCode;
    }

    public void setShortCode(String shortCode) {
      this.shortCode = shortCode;
    }

    public String getShortUrl() {
      return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
      this.shortUrl = shortUrl;
    }

    public ItemStatus getStatus() {
      return status;
    }

    public void setStatus(ItemStatus status) {
      this.status = status;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }
  }
}