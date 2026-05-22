package com.svenruppert.urlshortener.core.prefs;





import java.util.Map;

public record ColumnInfoResponse(
    String userId,
    String viewId,
    Map<String, Boolean> columnInfos) {
}