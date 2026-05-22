package com.svenruppert.urlshortener.core.urlmapping;





import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.Objects;

public final class ShortenRequest {
  private String url;
  @JsonAlias("alias")
  private String shortURL;
  private Instant expiresAt;
  private Boolean active;

  public ShortenRequest() {
  }

  public ShortenRequest(String url, String shortURL, Instant expiresAt, Boolean active) {
    this.url = url;
    this.shortURL = shortURL;
    this.expiresAt = expiresAt;
    this.active = active;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getShortURL() {
    return shortURL;
  }

  public void setShortURL(String shortURL) {
    this.shortURL = shortURL;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean hasAlias() {
    return shortURL != null && !shortURL.isBlank();
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @Override
  public String toString() {
    return "ShortenRequest{" +
        "url='" + url + '\'' +
        ", shortURL='" + shortURL + '\'' +
        ", expiresAt='" + expiresAt + '\'' +
        ", active='" + active + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ShortenRequest that = (ShortenRequest) o;
    return Objects.equals(url, that.url) && Objects.equals(shortURL, that.shortURL) && Objects.equals(expiresAt, that.expiresAt) && Objects.equals(active, that.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, shortURL, expiresAt, active);
  }
}