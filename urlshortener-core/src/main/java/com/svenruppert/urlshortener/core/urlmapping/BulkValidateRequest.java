package com.svenruppert.urlshortener.core.urlmapping;





import java.util.List;

/**
 * Request DTO for the bulk-validate endpoint.
 *
 * <p>{@code urls} contains the new URLs to validate (from the textarea input).
 * {@code existingUrls} lists URLs already present in the UI work set so the server
 * can detect DUPLICATE_IN_GRID conditions without the client having to pre-filter.
 */
public final class BulkValidateRequest {

  public static final int MAX_URLS = 500;
  public static final int MAX_URL_LENGTH = 2_000;

  /** New URLs submitted for validation. */
  private List<String> urls;

  /**
   * URLs already in the current work set (grid).
   * Used server-side to detect duplicates against existing entries.
   * May be {@code null} or empty when the work set is empty.
   */
  private List<String> existingUrls;

  public BulkValidateRequest() {
  }

  public BulkValidateRequest(List<String> urls, List<String> existingUrls) {
    this.urls = urls;
    this.existingUrls = existingUrls != null ? existingUrls : List.of();
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }

  public List<String> getExistingUrls() {
    return existingUrls;
  }

  public void setExistingUrls(List<String> existingUrls) {
    this.existingUrls = existingUrls;
  }
}