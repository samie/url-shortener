package com.svenruppert.urlshortener.ui.vaadin.security;



import com.svenruppert.dependencies.core.logger.HasLogger;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@WebListener
public class LoginConfigInitializer implements ServletContextListener, HasLogger {

  private static final String PROPERTIES_PATH = "auth.properties";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    logger().info("Initialising LoginConfig from {}", PROPERTIES_PATH);

    Properties props = new Properties();

    try (InputStream in = getClass()
        .getClassLoader()
        .getResourceAsStream(PROPERTIES_PATH)) {

      if (in == null) {
        logger().warn("No {} found on classpath. Login will be disabled.", PROPERTIES_PATH);
        LoginConfig.initialise(false, null);
        return;
      }

      props.load(in);

      String enabledRaw = props.getProperty("login.enabled", "true").trim().toLowerCase();
      boolean enabled = switch (enabledRaw) {
        case "true", "yes", "y", "1", "on" -> true;
        case "false", "no", "n", "0", "off" -> false;
        default -> Boolean.parseBoolean(enabledRaw);
      };

      String password = props.getProperty("login.password");

      LoginConfig.initialise(enabled, password);

      if (!enabled) {
        logger().info("Login explicitly disabled via login.enabled=false");
      } else if (LoginConfig.isLoginConfigured()) {
        logger().info("LoginConfig initialised successfully from {}", PROPERTIES_PATH);
      } else {
        logger().warn("login.enabled=true but no usable password configured. "
                          + "Login will effectively be disabled.");
      }

    } catch (IOException e) {
      logger().error("Failed to load " + PROPERTIES_PATH + ". Login will be disabled.", e);
      LoginConfig.initialise(false, null);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // Nothing to clean up
  }
}