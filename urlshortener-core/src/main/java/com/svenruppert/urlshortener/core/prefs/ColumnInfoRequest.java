package com.svenruppert.urlshortener.core.prefs;





/**
 * Represents a request to retrieve column information for a specific user and view.
 * This class defines the minimal required input data to fetch column-related configuration
 * or preferences for a given user's specific view context.
 * Typically used in scenarios where column visibility or other configurations
 * need to be queried or processed based on the user and view identifiers.
 *
 * @param userId The identifier of the user for whom the column information is requested.
 * @param viewId The identifier of the view for which the column information is requested.
 */
public record ColumnInfoRequest(
    String userId,
    String viewId) {
}