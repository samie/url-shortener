package com.svenruppert.urlshortener.ui.vaadin.components;



public enum ActiveState {
  ACTIVE,
  INACTIVE,
  NOT_SET;

  public static ActiveState fromBoolean(Boolean value) {
    if (value == null) return NOT_SET;
    return value ? ACTIVE : INACTIVE;
  }

  public Boolean toBoolean() {
    return switch (this) {
      case ACTIVE -> Boolean.TRUE;
      case INACTIVE -> Boolean.FALSE;
      case NOT_SET -> null;
    };
  }

  public boolean isSet() {
    return this != NOT_SET;
  }

  public boolean isActive() {
    return this == ACTIVE;
  }
}