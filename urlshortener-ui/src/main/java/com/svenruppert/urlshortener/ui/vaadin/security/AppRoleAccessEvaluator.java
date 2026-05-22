package com.svenruppert.urlshortener.ui.vaadin.security;



import com.svenruppert.urlshortener.ui.vaadin.views.login.LoginView;
import com.svenruppert.urlshortener.ui.vaadin.views.overview.OverviewView;
import com.svenruppert.vaadin.security.authorization.api.AccessEvaluator;
import com.svenruppert.vaadin.security.authorization.api.AuthorizationService;
import com.svenruppert.vaadin.security.authorization.api.SecurityServiceResolver;
import com.svenruppert.vaadin.security.authorization.api.SessionAccessor;
import com.svenruppert.vaadin.security.authorization.api.roles.RoleName;
import com.svenruppert.vaadin.security.authorization.navigation.AuthorizationDecision;
import com.vaadin.flow.router.Location;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AppRoleAccessEvaluator implements AccessEvaluator<VisibleFor> {

  @Override
  public AuthorizationDecision evaluateAccess(
      Location location,
      Class<?> navigationTarget,
      VisibleFor annotation) {

    Set<RoleName> requiredRoles = Arrays.stream(annotation.value())
        .map(Enum::name)
        .map(RoleName::new)
        .collect(Collectors.toSet());

    if (requiredRoles.isEmpty()) {
      return AuthorizationDecision.granted();
    }

    var currentSubject = SessionAccessor.<AppUser>currentSubject();
    if (currentSubject.isAbsent()) {
      return AuthorizationDecision.denied(LoginView.PATH, false);
    }

    AuthorizationService<AppUser> authorizationService =
        SecurityServiceResolver.authorizationService();

    boolean hasRole = authorizationService.rolesFor(currentSubject.get())
        .roleNames()
        .stream()
        .anyMatch(requiredRoles::contains);

    if (hasRole) {
      return AuthorizationDecision.granted();
    }

    return AuthorizationDecision.denied(OverviewView.PATH, true);
  }
}