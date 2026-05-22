package com.svenruppert.urlshortener.ui.vaadin.views.login;



import com.svenruppert.urlshortener.ui.vaadin.security.AppCredentials;
import com.svenruppert.urlshortener.ui.vaadin.security.AppUser;
import com.svenruppert.urlshortener.ui.vaadin.security.LoginConfig;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.svenruppert.urlshortener.ui.vaadin.views.overview.OverviewView;
import com.svenruppert.vaadin.security.authorization.api.AuthenticationService;
import com.svenruppert.vaadin.security.authorization.api.SecurityServiceResolver;
import com.svenruppert.vaadin.security.authorization.api.SessionAccessor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

@Route(LoginView.PATH)
public class LoginView
    extends com.svenruppert.vaadin.security.authorization.LoginView
    implements I18nSupport, HasDynamicTitle {

  public static final String PATH = "login";

  private static final String K_PAGE_TITLE = "login.pageTitle";
  private static final String K_ERR_INCORRECT_PW = "login.error.incorrectPassword";

  private final AuthenticationService<AppCredentials, AppUser> authenticationService =
      SecurityServiceResolver.authenticationService();

  @Override
  public boolean checkCredentials() {
    if (!LoginConfig.isLoginEnabled()) {
      Notifications.loginCurrentlyDisabled();
      return false;
    }
    if (!LoginConfig.isLoginConfigured()) {
      Notifications.loginCurrentlyNotConfigured();
      return false;
    }

    AppCredentials credentials = new AppCredentials(username(), password());
    boolean permitted = authenticationService.checkCredentials(credentials);

    if (permitted) {
      AppUser user = authenticationService.loadSubject(credentials);
      SessionAccessor.setCurrentSubject(user);
    }

    return permitted;
  }

  @Override
  public void navigateToApp() {
    UI.getCurrent().navigate(OverviewView.class);
  }

  @Override
  public void reactOnFailedLogin() {
    Notifications.errorKey(K_ERR_INCORRECT_PW, "Incorrect password");
  }

  @Override
  public String getPageTitle() {
    return tr(K_PAGE_TITLE, "Admin Login | URL Shortener");
  }
}