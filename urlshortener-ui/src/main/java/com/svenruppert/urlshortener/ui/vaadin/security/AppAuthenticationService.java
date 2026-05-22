package com.svenruppert.urlshortener.ui.vaadin.security;



import com.svenruppert.vaadin.security.authorization.api.AuthenticationService;

import java.util.Set;

public class AppAuthenticationService
    implements AuthenticationService<AppCredentials, AppUser> {

  static final String DEFAULT_USERNAME = "admin";

  @Override
  public boolean checkCredentials(AppCredentials credentials) {
    if (credentials == null || credentials.password() == null) return false;
    if (!LoginConfig.isLoginConfigured()) return false;
    return LoginConfig.matches(credentials.password().toCharArray());
  }

  @Override
  public AppUser loadSubject(AppCredentials credentials) {
    String name = (credentials != null && credentials.username() != null
        && !credentials.username().isBlank())
        ? credentials.username()
        : DEFAULT_USERNAME;
    return new AppUser(name, Set.of(AppRole.USER));
  }

  @Override
  public Class<AppUser> subjectType() {
    return AppUser.class;
  }
}