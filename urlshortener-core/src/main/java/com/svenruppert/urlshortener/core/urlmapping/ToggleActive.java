package com.svenruppert.urlshortener.core.urlmapping;





public class ToggleActive {
  public record ToggleActiveRequest(String shortCode, boolean active) {
  }

  public record ToggleActiveResponse(String shortCode, boolean active) {
  }
}