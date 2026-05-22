package com.svenruppert.urlshortener.api;

import com.sun.net.httpserver.HttpServer;
import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.api.filter.BlockBrowserPreflightFilter;
import com.svenruppert.urlshortener.api.handler.RedirectHandler;
import com.svenruppert.urlshortener.api.handler.admin.StoreInfoHandler;
import com.svenruppert.urlshortener.api.handler.admin.columns.ColumnVisibilityBulkHandler;
import com.svenruppert.urlshortener.api.handler.admin.columns.ColumnVisibilityHandler;
import com.svenruppert.urlshortener.api.handler.admin.columns.ColumnVisibilitySingleHandler;
import com.svenruppert.urlshortener.api.handler.statistics.*;
import com.svenruppert.urlshortener.api.handler.statistics.exports.StatisticsExportHandler;
import com.svenruppert.urlshortener.api.handler.statistics.imports.StatisticsImportHandler;
import com.svenruppert.urlshortener.api.handler.urlmapping.*;
import com.svenruppert.urlshortener.api.handler.urlmapping.imports.ImportApplyHandler;
import com.svenruppert.urlshortener.api.handler.urlmapping.imports.ImportConflictsListHandler;
import com.svenruppert.urlshortener.api.handler.urlmapping.imports.ImportInvalidListHandler;
import com.svenruppert.urlshortener.api.handler.urlmapping.imports.ImportValidateHandler;
import com.svenruppert.urlshortener.api.store.imports.ImportStagingStore;
import com.svenruppert.urlshortener.api.store.imports.InMemoryImportStagingStore;
import com.svenruppert.urlshortener.api.store.preferences.PreferencesStore;
import com.svenruppert.urlshortener.api.store.provider.eclipsestore.EclipseStore;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryPreferencesStore;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryStatisticsStore;
import com.svenruppert.urlshortener.api.store.provider.inmemory.InMemoryUrlMappingStore;
import com.svenruppert.urlshortener.api.store.statistics.StatisticsStore;
import com.svenruppert.urlshortener.api.store.urlmapping.UrlMappingStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static com.svenruppert.urlshortener.core.DefaultValues.*;

public class ShortenerServer
    implements HasLogger {

  private HttpServer serverRedirect;
  private HttpServer serverAdmin;

  public static void main(String[] args)
      throws IOException {
    // Defaults (Fallbacks)
    String host = DEFAULT_SERVER_HOST;
    int port = DEFAULT_SERVER_PORT;
    HasLogger.staticLogger().info("Main called with arguments: {}", Arrays.toString(args));

    // Read parameters from CLI
    if (args.length > 0 && !args[0].isBlank()) {
      host = args[0].trim();
      HasLogger.staticLogger().info("Host is {}", host);
    }
    if (args.length > 1) {
      try {
        port = Integer.parseInt(args[1].trim());
      } catch (NumberFormatException e) {
        HasLogger.staticLogger().warn("Invalid port argument: {} - using default {}", args[1], port);
      }
    }


    boolean persistent = true;
    new ShortenerServer().init(host, port, persistent);
  }

  public void init()
      throws IOException {
    logger().info("Starting URL Shortener server with default parameters...");
    init(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
  }

  public void init(String hostRedirect, int portRedirect)
      throws IOException {
    init(hostRedirect, portRedirect, false);
  }

  public void init(String hostRedirect, int portRedirect, boolean persistent)
      throws IOException {
    logger().info("Starting server with urlMappingStore - persistent={}", persistent);
    final long startedAt = System.currentTimeMillis();
    var shortCodeGenerator = new ShortCodeGenerator(1);

    UrlMappingStore urlMappingStore;
    PreferencesStore preferencesStore;
    StatisticsStore statisticsStore;
    ImportStagingStore importStagingStore = new InMemoryImportStagingStore();

    if (persistent) {
      var eclipseStore = new EclipseStore(
          STORAGE_DATA_PATH,
          shortCodeGenerator,
          Clock.systemUTC()
      );
      urlMappingStore = eclipseStore.getUrlMappingStore();
      preferencesStore = eclipseStore.getPreferencesStore();
      statisticsStore = eclipseStore.getStatisticsStore();

      // Start background threads for statistics processing
      eclipseStore.start();
      logger().info("EclipseStore started with statistics processing");

      logger().info("Registering shutdown hook for EclipseStore...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        shutdown();
        logger().info("Closing EclipseStore...");
        try {
          eclipseStore.close();
        } catch (Exception ignored) {
        }
        logger().info("Server shutdown complete");
      }));
    } else {
      urlMappingStore = new InMemoryUrlMappingStore(shortCodeGenerator);
      preferencesStore = new InMemoryPreferencesStore();
      statisticsStore = new InMemoryStatisticsStore(Clock.systemUTC());
      // statisticsStore remains null for in-memory mode
    }

    logger().info("Starting URL Shortener server (redirect) with parameters: host={}, port={}", hostRedirect, portRedirect);
    this.serverRedirect = HttpServer.create(new InetSocketAddress(hostRedirect, portRedirect), 0);
    serverRedirect.createContext(PATH_REDIRECT, new RedirectHandler(urlMappingStore, statisticsStore));

    logger().info("Starting URL Shortener server (admin) with parameters: host={}, port={}", ADMIN_SERVER_HOST, ADMIN_SERVER_PORT);
    this.serverAdmin = HttpServer.create(new InetSocketAddress(ADMIN_SERVER_HOST, ADMIN_SERVER_PORT), 0);
    serverAdmin.createContext(PATH_ADMIN_VALIDATE_BULK, new BulkValidateHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_SHORTEN_BULK, new BulkShortenHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_SHORTEN, new ShortenHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_LIST, new ListHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_LIST_COUNT, new ListCountHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_EDIT, new EditMappingHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_DELETE, new DeleteMappingHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_TOGGLE_ACTIVE, new ToggleActiveHandler(urlMappingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STORE_INFO, new StoreInfoHandler(urlMappingStore, startedAt)).getFilters().add(new BlockBrowserPreflightFilter());

    serverAdmin.createContext(PATH_ADMIN_IMPORT_VALIDATE, new ImportValidateHandler(urlMappingStore, importStagingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_IMPORT_APPLY, new ImportApplyHandler(urlMappingStore, importStagingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_IMPORT_CONFLICTS, new ImportConflictsListHandler(importStagingStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_IMPORT_INVALID, new ImportInvalidListHandler(importStagingStore)).getFilters().add(new BlockBrowserPreflightFilter());

    serverAdmin.createContext(PATH_ADMIN_PREFERENCES_COLUMNS, new ColumnVisibilityHandler(preferencesStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_PREFERENCES_COLUMNS_EDIT, new ColumnVisibilityBulkHandler(preferencesStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_PREFERENCES_COLUMNS_SINGLE, new ColumnVisibilitySingleHandler(preferencesStore)).getFilters().add(new BlockBrowserPreflightFilter());

    serverAdmin.createContext(PATH_ADMIN_STATISTICS_COUNT, new StatisticsCountHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_HOURLY, new StatisticsHourlyHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_DAILY, new StatisticsDailyHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_TIMELINE, new StatisticsTimelineHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_CONFIG, new StatisticsConfigHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_DEBUG, new StatisticsDebugHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_EXPORT, new StatisticsExportHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    serverAdmin.createContext(PATH_ADMIN_STATISTICS_IMPORT, new StatisticsImportHandler(statisticsStore)).getFilters().add(new BlockBrowserPreflightFilter());
    logger().info("Statistics API handlers registered");


    var execRedirect = Executors.newVirtualThreadPerTaskExecutor();
    serverRedirect.setExecutor(execRedirect);
    serverRedirect.start();

    var execAdmin = Executors.newVirtualThreadPerTaskExecutor();
    serverAdmin.setExecutor(execAdmin);
    serverAdmin.start();

    logger().info("URL Shortener server (redirect) running at {}:{}",
                  serverRedirect.getAddress().getHostName(),
                  serverRedirect.getAddress().getPort());
    logger().info("URL Shortener server (admin) running at {}:{}",
                  serverAdmin.getAddress().getHostName(),
                  serverAdmin.getAddress().getPort());


    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  public void shutdown() {
    if (serverRedirect != null) {
      serverRedirect.stop(0);
      logger().info("URL Shortener server stopped (redirect)");
    }
    if (serverAdmin != null) {
      serverAdmin.stop(0);
      logger().info("URL Shortener server stopped (admin)");
    }
  }

  public int getPortRedirect() {
    return serverRedirect.getAddress().getPort();
  }

  public int getPortAdmin() {
    return serverAdmin.getAddress().getPort();
  }


}