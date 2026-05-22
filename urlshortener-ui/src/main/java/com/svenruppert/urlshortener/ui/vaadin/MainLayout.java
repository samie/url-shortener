package com.svenruppert.urlshortener.ui.vaadin;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.ui.vaadin.components.StoreIndicator;
import com.svenruppert.urlshortener.ui.vaadin.security.LoginConfig;
import com.svenruppert.urlshortener.ui.vaadin.tools.AdminClientFactory;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.tools.LocaleSelection;
import com.svenruppert.urlshortener.ui.vaadin.views.AboutView;
import com.svenruppert.urlshortener.ui.vaadin.views.BulkCreateView;
import com.svenruppert.urlshortener.ui.vaadin.views.CreateView;
import com.svenruppert.urlshortener.ui.vaadin.views.YoutubeView;
import com.svenruppert.urlshortener.ui.vaadin.views.login.LoginView;
import com.svenruppert.urlshortener.ui.vaadin.views.overview.OverviewView;
import com.svenruppert.urlshortener.ui.vaadin.views.statistics.StatisticsView;
import com.svenruppert.vaadin.security.authorization.api.SessionAccessor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Locale;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

@CssImport("./styles/main-layout.css")
public class MainLayout
    extends AppLayout
    implements HasLogger, I18nSupport {

  private static final String C_MAINLAYOUT = "mainlayout";
  private static final String C_APP_TITLE = "title";
  private static final String C_HEADER_ROW = "header";
  private static final String C_SPACER = "spacer";
  private static final String C_RIGHT = "right";

  // i18n keys
  private static final String K_APP_TITLE = "main.appTitle";
  private static final String K_LOGOUT = "main.logout";

  private static final String K_NAV_OVERVIEW = "nav.overview";
  private static final String K_NAV_CREATE = "nav.create";
  private static final String K_NAV_BULK_CREATE = "nav.bulkCreate";
  private static final String K_NAV_STATISTICS = "nav.statistics";
  private static final String K_NAV_YOUTUBE = "nav.youtube";
  private static final String K_NAV_ABOUT = "nav.about";

  Component languageSwitch = createLanguageSwitch();


  public MainLayout() {
    createHeader();
    this.setClassName(C_MAINLAYOUT);
  }

  private void createHeader() {
    H1 appTitle = new H1(tr(K_APP_TITLE, "URL Shortener"));
    appTitle.addClassName(C_APP_TITLE);

    SideNav views = getPrimaryNavigation();
    Scroller scroller = new Scroller(views);
    scroller.setClassName(LumoUtility.Padding.SMALL);

    DrawerToggle toggle = new DrawerToggle();

    var adminClient = AdminClientFactory.newInstance();
    var storeIndicator = new StoreIndicator(adminClient);
    storeIndicator.addClassName(C_RIGHT);

    HorizontalLayout headerRow;

    Span spacer = new Span();
    spacer.addClassName(C_SPACER);

    if (LoginConfig.isLoginEnabled()) {
      var logoutButton = new Button(tr(K_LOGOUT, "Logout"), _ -> {
        SessionAccessor.deleteCurrentSubject();
        UI.getCurrent().navigate(LoginView.class);
      });
      logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      headerRow = new HorizontalLayout(toggle, appTitle, spacer, languageSwitch, storeIndicator, logoutButton);
    } else {
      headerRow = new HorizontalLayout(toggle, appTitle, spacer, languageSwitch, storeIndicator);
    }

    headerRow.addClassName(C_HEADER_ROW);
    headerRow.setWidthFull();
    headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
    headerRow.setSpacing(true);
    headerRow.setPadding(true);
    headerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

    VerticalLayout viewHeader = new VerticalLayout(headerRow);
    viewHeader.setPadding(false);
    viewHeader.setSpacing(false);

    addToDrawer(appTitle, scroller);
    addToNavbar(viewHeader);

    setPrimarySection(Section.DRAWER);
  }

  private SideNav getPrimaryNavigation() {
    SideNav sideNav = new SideNav();
    sideNav.addItem(
        new SideNavItem(tr(K_NAV_OVERVIEW, "Overview"), "/" + OverviewView.PATH, DASHBOARD.create()),
        new SideNavItem(tr(K_NAV_CREATE, "Create"), "/" + CreateView.PATH, DASHBOARD.create()),
        new SideNavItem(tr(K_NAV_BULK_CREATE, "Bulk Create"), "/" + BulkCreateView.PATH, COPY_O.create()),
        new SideNavItem(tr(K_NAV_STATISTICS, "Statistics"), "/" + StatisticsView.PATH, CHART.create()),
        new SideNavItem(tr(K_NAV_YOUTUBE, "Youtube"), "/" + YoutubeView.PATH, CART.create()),
        new SideNavItem(tr(K_NAV_ABOUT, "About"), "/" + AboutView.PATH, USER_HEART.create())
    );
    return sideNav;
  }

  private Component createLanguageSwitch() {

    Locale current = LocaleSelection.resolveAndStore(
        VaadinSession.getCurrent(),
        UI.getCurrent().getLocale()
    );

    Button de = flagButton(LocaleSelection.DE, current);
    Button en = flagButton(LocaleSelection.EN, current);
    Button fi = flagButton(LocaleSelection.FI, current);

    HorizontalLayout bar = new HorizontalLayout(de, en, fi);
    bar.setSpacing(false);
    bar.getStyle()
        .set("gap", "6px")
        .set("padding", "2px")
        .set("border-radius", "999px")
        .set("background", "var(--lumo-contrast-5pct)");

    return bar;
  }

  private Button flagButton(Locale locale, Locale current) {

    String flag = flagEmoji(locale);

    Button b = new Button(new Span(flag));
    b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    b.getStyle()
        .set("width", "38px")
        .set("height", "32px")
        .set("min-width", "38px")
        .set("border-radius", "999px")
        .set("padding", "0")
        .set("line-height", "1")
        .set("font-size", "18px");

    boolean active = locale.getLanguage().equalsIgnoreCase(current.getLanguage());
    applyActiveStyle(b, active);

    b.addClickListener(e -> switchLocale(locale));

    b.getElement().setProperty("title", locale.getLanguage().toUpperCase());

    return b;
  }

  private void applyActiveStyle(Button b, boolean active) {
    if (active) {
      b.getStyle()
          .set("background", "var(--lumo-primary-color-10pct)")
          .set("outline", "2px solid var(--lumo-primary-color-50pct)");
    } else {
      b.getStyle()
          .remove("background")
          .remove("outline");
    }
  }

  private void switchLocale(Locale selected) {
    UI ui = UI.getCurrent();
    VaadinSession session = VaadinSession.getCurrent();

    Locale effective = LocaleSelection.match(selected);

    LocaleSelection.setToSession(session, effective);
    session.setLocale(effective);
    ui.setLocale(effective);

    ui.getPage().reload();
  }

  private String flagEmoji(Locale locale) {
    return switch (locale.getLanguage()) {
      case "de" -> "🇩🇪";
      case "fi" -> "🇫🇮";
      default -> "🇬🇧"; // EN
    };
  }


}