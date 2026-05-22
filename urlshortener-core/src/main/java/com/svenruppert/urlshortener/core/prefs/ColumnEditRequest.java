package com.svenruppert.urlshortener.core.prefs;





import java.util.Map;

public record ColumnEditRequest(String userId, String viewId, Map<String, Boolean> changes) {
}