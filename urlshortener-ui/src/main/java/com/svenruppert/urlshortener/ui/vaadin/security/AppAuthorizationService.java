package com.svenruppert.urlshortener.ui.vaadin.security;



import com.svenruppert.vaadin.security.authorization.api.AuthorizationService;
import com.svenruppert.vaadin.security.authorization.api.roles.HasRoles;
import com.svenruppert.vaadin.security.authorization.api.roles.RoleName;

import java.util.Objects;

public class AppAuthorizationService
    implements AuthorizationService<AppUser> {

  @Override
  public HasRoles rolesFor(AppUser subject) {
    Objects.requireNonNull(subject);
    return () -> subject.roles()
        .stream()
        .map(role -> new RoleName(role.name()))
        .toList();
  }
}