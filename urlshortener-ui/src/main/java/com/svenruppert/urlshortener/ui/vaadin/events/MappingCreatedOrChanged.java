package com.svenruppert.urlshortener.ui.vaadin.events;



import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

public class MappingCreatedOrChanged extends ComponentEvent<Component> {
  public MappingCreatedOrChanged(Component source) {
    super(source, false);
  }
}