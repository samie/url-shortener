package com.svenruppert.urlshortener.core;





public record StoreInfo(
    String mode, // "InMemory" | "EclipseStore"
    int mappings, // current number
    long startedAtEpochMs // server start time (for diagnostics)
) { }