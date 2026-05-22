package com.svenruppert.urlshortener.core.prefs;





public record ColumnSingleEditRequest(String userId, String viewId, String columnKey, boolean visible) {
}