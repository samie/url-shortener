package com.svenruppert.urlshortener.ui.vaadin.security;



import com.svenruppert.vaadin.security.authorization.annotations.NavigationAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@NavigationAnnotation(AppRoleAccessEvaluator.class)
public @interface VisibleFor {
  AppRole[] value();
}