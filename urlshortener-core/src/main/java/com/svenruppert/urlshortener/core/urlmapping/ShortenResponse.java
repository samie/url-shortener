package com.svenruppert.urlshortener.core.urlmapping;





public record ShortenResponse(String shortCode,
                              String originalUrl) {
  public static ShortenResponse from(ShortUrlMapping m) {
    return new ShortenResponse(m.shortCode(), m.originalUrl());
  }
}