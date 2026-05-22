package com.svenruppert.urlshortener.ui.vaadin;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * Use the @PWA annotation to make the application installable on phones, tablets
 * and some desktop browsers.
 */
@PWA(name = "An OpenSource URL Shortener", shortName = "URL Shortener")
@Theme("my-theme")
@Push
public class AppShell
    implements AppShellConfigurator, HasLogger {

  @Override
  public void configurePage(AppShellSettings settings) {
    AppShellConfigurator.super.configurePage(settings);

    logger().info("configurePage .. start to init the Page..");


    logger().info("configurePage .. page init done.");
  }
}