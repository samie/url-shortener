package com.svenruppert.urlshortener.core.prefs;





import java.io.Serializable;
import java.util.Map;

/**
 * Represents a user's preferences for column visibility in a specific view.
 * This class encapsulates the details of which columns should be visible or hidden
 * for a particular user in a given view context.
 * This is a serializable class, making it suitable for storage or transmission
 * over the network. It contains information about the user and the view it applies to,
 * as well as a map that associates column identifiers with their visibility status.
 */
public class ColumnVisibilityPrefs
    implements Serializable {

  private String userId;
  private String viewId;
  private Map<String, Boolean> visibility;

  @Override
  public String toString() {
    return "ColumnVisibilityPrefs{" +
        "userId='" + userId + '\'' +
        ", viewId='" + viewId + '\'' +
        ", visibility=" + visibility +
        '}';
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getViewId() {
    return viewId;
  }

  public void setViewId(String viewId) {
    this.viewId = viewId;
  }

  public Map<String, Boolean> getVisibility() {
    return visibility;
  }

  public void setVisibility(Map<String, Boolean> visibility) {
    this.visibility = visibility;
  }
}