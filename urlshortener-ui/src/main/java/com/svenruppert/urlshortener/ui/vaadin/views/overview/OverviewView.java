package com.svenruppert.urlshortener.ui.vaadin.views.overview;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.ColumnVisibilityClient;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.core.urlmapping.UrlMappingListRequest;
import com.svenruppert.urlshortener.ui.vaadin.MainLayout;
import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.VisibleFor;
import com.svenruppert.urlshortener.ui.vaadin.components.ColumnVisibilityDialog;
import com.svenruppert.urlshortener.ui.vaadin.components.SearchBar;
import com.svenruppert.urlshortener.ui.vaadin.events.MappingCreatedOrChanged;
import com.svenruppert.urlshortener.ui.vaadin.events.StoreEvents;
import com.svenruppert.urlshortener.ui.vaadin.tools.*;
import com.svenruppert.urlshortener.ui.vaadin.tools.UiLinks;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.svenruppert.urlshortener.ui.vaadin.views.overview.imports.ImportDialog;
import com.svenruppert.urlshortener.ui.vaadin.views.statistics.StatisticsDetailView;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.svenruppert.urlshortener.core.DefaultValues.*;
import static com.svenruppert.urlshortener.ui.vaadin.components.ExpiryBadgeFactory.computeStatusText;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;

@PageTitle("Overview")
@Route(value = OverviewView.PATH, layout = MainLayout.class)
@VisibleFor(AppRole.USER)
@CssImport("./styles/overview-view.css")
public class OverviewView
    extends VerticalLayout
    implements HasLogger, HasRefreshGuard, I18nSupport {

  public static final String PATH = "";
  protected static final int VALUE_CHANGE_TIMEOUT = 400;

  private static final DateTimeFormatter DATE_TIME_FMT =
      DateTimeFormatter.ofPattern(PATTERN_DATE_TIME)
          .withLocale(Locale.GERMANY)
          .withZone(ZoneId.systemDefault());

  // CSS classes
  private static final String C_ROOT = "overview-view";
  private static final String C_CONTAINER = "overview-view__container";
  private static final String C_BOTTOM_BAR = "overview-view__bottombar";
  private static final String C_PAGING_BAR = "overview-view__pagingbar";
  private static final String C_EXPORT_ANCHOR = "overview-view__export-anchor";
  private static final String C_GRID = "overview-view__grid";
  private static final String C_URL_ANCHOR = "overview-view__url";
  private static final String C_SHORTCODE = "overview-view__shortcode";
  private static final String C_SHORTCODE_WRAP = "overview-view__shortcode-wrap";
  private static final String C_ROW_ACTIONS = "overview-view__row-actions";
  private static final String C_ACTIVE_ICON = "overview-view__active-icon";

  // i18n keys (Overview is leading)
  private static final String K_OVERVIEW_H2 = "overview.h2";

  private static final String K_PAGING_PREV = "overview.paging.prev";
  private static final String K_PAGING_NEXT = "overview.paging.next";
//  private static final String K_PAGING_PAGE = "overview.paging.page";
//  private static final String K_PAGING_TOTAL = "overview.paging.total";

  private static final String K_EXPORT = "overview.export";
  private static final String K_EXPORT_TOOLTIP = "overview.export.tooltip";
  private static final String K_EXPORT_ANCHOR = "overview.export.anchor";
  private static final String K_IMPORT = "overview.import";

  private static final String K_DOWNLOAD_STARTED = "overview.download.started";
  private static final String K_DOWNLOAD_COMPLETED = "overview.download.completed";
  private static final String K_DOWNLOAD_FAILED = "overview.download.failed";

  private static final String K_SELECTION_ONE = "overview.selection.one";
  private static final String K_SELECTION_MANY = "overview.selection.many";

  private static final String K_MENU_SHOW_DETAILS = "overview.menu.showDetails";
  private static final String K_MENU_OPEN_URL = "overview.menu.openUrl";
  private static final String K_MENU_COPY_SHORTCODE = "overview.menu.copyShortcode";
  private static final String K_MENU_DELETE = "overview.menu.delete";

  private static final String K_GRID_COL_SHORTCODE = "overview.grid.column.shortcode";
  private static final String K_GRID_COL_URL = "overview.grid.column.url";
  private static final String K_GRID_COL_CREATED = "overview.grid.column.created";
  private static final String K_GRID_COL_ACTIVE = "overview.grid.column.active";
  private static final String K_GRID_COL_EXPIRES = "overview.grid.column.expires";
  private static final String K_GRID_COL_ACTIONS = "overview.grid.column.actions";

  private static final String K_ACTIVE_TOOLTIP_ACTIVATE = "overview.active.tooltip.activate";
  private static final String K_ACTIVE_TOOLTIP_DEACTIVATE = "overview.active.tooltip.deactivate";

  private static final String K_COPY_SHORTURL_TOOLTIP = "overview.shortcode.copy.tooltip";
  private static final String K_STATISTICS_TOOLTIP = "overview.statistics.tooltip";

  private static final String K_DELETE_TITLE = "overview.delete.title";
//  private static final String K_DELETE_QUESTION_PREFIX = "overview.delete.question.prefix";
//  private static final String K_DELETE_QUESTION_SUFFIX = "overview.delete.question.suffix";

  private static final String K_COMMON_DELETE = "common.delete";
  private static final String K_COMMON_CANCEL = "common.cancel";

  private final URLShortenerClient urlShortenerClient = UrlShortenerClientFactory.newInstance();
  private final ColumnVisibilityClient columnVisibilityClient = ColumnVisibilityClientFactory.newInstance();
  private final ColumnVisibilityService columnVisibilityService =
      new ColumnVisibilityService(columnVisibilityClient, "admin", "overview");

  private final Grid<ShortUrlMapping> grid = new Grid<>(ShortUrlMapping.class, false);
  private final BulkActionsBar bulkBar = new BulkActionsBar(urlShortenerClient, grid, this);
  private final Button btnSettings = new Button(new Icon(VaadinIcon.COG));
  private final SearchBar searchBar = new SearchBar();

  // NOTE: no text in field initializer (i18n happens later)
  private final Button prevBtn = new Button();
  private final Button nextBtn = new Button();
  private final Text pageInfo = new Text("");

  private final Button btnExport = new Button(new Icon(VaadinIcon.DOWNLOAD));
  private final Anchor exportAnchor = initAnchor();
  private final Button btnImport = new Button(new Icon(VaadinIcon.UPLOAD));

  private Integer currentPage = 1;
  private Integer totalCount = 0;
  private CallbackDataProvider<ShortUrlMapping, Void> dataProvider;
  private AutoCloseable subscription;
  private volatile boolean suppressRefresh = false;

  public OverviewView() {
    addClassName(C_ROOT);

    setSizeFull();
    setPadding(true);
    setSpacing(true);

    // headline is translated in applyI18n()
    add(new H2(tr(K_OVERVIEW_H2, "URL Shortener – Overview")));

    initDataProvider();

    btnExport.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    btnExport.addClickListener(_ -> triggerExportDownload());

    btnImport.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    btnImport.addClickListener(_ -> new ImportDialog(urlShortenerClient, this::safeRefresh).open());

    exportAnchor.addClassName(C_EXPORT_ANCHOR);

    var pagingBar = new HorizontalLayout(prevBtn, nextBtn, pageInfo, btnImport, btnExport, btnSettings);
    pagingBar.addClassName(C_PAGING_BAR);
    pagingBar.setDefaultVerticalComponentAlignment(CENTER);

    HorizontalLayout bottomBar = new HorizontalLayout(new Span(), pagingBar);
    bottomBar.addClassName(C_BOTTOM_BAR);
    bottomBar.setWidthFull();
    bottomBar.expand(bottomBar.getComponentAt(0));
    bottomBar.setAlignItems(CENTER);

    VerticalLayout container = new VerticalLayout(searchBar, bottomBar, exportAnchor);
    container.addClassName(C_CONTAINER);
    container.setPadding(false);
    container.setSpacing(true);
    container.setWidthFull();

    add(container, bulkBar, grid);

    configureGrid();
    applyI18n();      // <- central place for all UI text
    addListeners();
    addShortCuts();

    try (var _ = withRefreshGuard(false)) {
      searchBar.setPageSize(25);
      searchBar.setSortBy("createdAt");
      searchBar.setDirValue("desc");

      // Configure SearchBar callbacks
      searchBar.setOnFilterChange(_ -> {
        currentPage = 1;
        safeRefresh();
      });
      searchBar.setOnPageSizeChange(this::setGridPageSize);
      searchBar.setOnReset(_ -> currentPage = 1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void applyI18n() {
    prevBtn.setText(tr(K_PAGING_PREV, "‹ Prev"));
    nextBtn.setText(tr(K_PAGING_NEXT, "Next ›"));

    btnExport.setText(tr(K_EXPORT, "Export"));
    btnExport.getElement().setAttribute("title", tr(K_EXPORT_TOOLTIP, "Export current result set as ZIP"));

    // Anchor text is visible; keep it meaningful
    exportAnchor.setText(tr(K_EXPORT_ANCHOR, "Download"));

    btnImport.setText(tr(K_IMPORT, "Import"));

    // initial paging string
    refreshPageInfo();
  }

  private Anchor initAnchor() {
    DownloadHandler downloadHandler =
        DownloadHandler.fromInputStream(_ -> {
              int chunkSize = Optional.ofNullable(searchBar.getPageSize()).orElse(500);
              chunkSize = Math.max(1, Math.min(500, chunkSize));
              UrlMappingListRequest filter = searchBar.buildFilter(1, chunkSize);
              URLShortenerClient.ExportZipDownload download = urlShortenerClient.exportAllAsZipDownload(filter);
              return new DownloadResponse(download.inputStreamFactory().get(), download.filename(), APPLICATION_ZIP, -1);
            })
            .whenStart(() ->
                           Notification.show(tr(K_DOWNLOAD_STARTED, "Download started"),
                                             3000, Notification.Position.BOTTOM_START))
            .whenComplete(success -> {
              if (success) {
                Notification.show(tr(K_DOWNLOAD_COMPLETED, "Download completed"),
                                  3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              } else {
                Notification.show(tr(K_DOWNLOAD_FAILED, "Download failed"),
                                  3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });

    var anchor = new Anchor(downloadHandler, "");
    anchor.getElement().setAttribute("download", true);
    anchor.addClassName(C_EXPORT_ANCHOR);
    return anchor;
  }

  private void triggerExportDownload() {
    try {
      exportAnchor.getElement().callJsFunction("click");
    } catch (Exception ex) {
      logger().warn("Export failed", ex);
      Notifications.operationFailed(ex);
    }
  }

  private void addListeners() {
    ComponentUtil.addListener(UI.getCurrent(), MappingCreatedOrChanged.class, _ -> {
      refreshPageInfo();
      safeRefresh();
    });

    grid.addSelectionListener(event -> {
      var all = event.getAllSelectedItems();
      boolean hasSelection = !all.isEmpty();

      bulkBar.setVisible(hasSelection);

      if (hasSelection) {
        int count = all.size();
        String label = (count == 1)
            ? tr(K_SELECTION_ONE, "link selected")
            : tr(K_SELECTION_MANY, "links selected");
        bulkBar.selectionInfoText(count + " " + label);
      } else {
        bulkBar.selectionInfoText("");
      }

      bulkBar.setButtonsEnabled(hasSelection);
    });

    btnSettings.addClickListener(_ -> new ColumnVisibilityDialog<>(grid, columnVisibilityService).open());

    prevBtn.addClickListener(_ -> {
      if (currentPage > 1) {
        currentPage--;
        refreshPageInfo();
        safeRefresh();
      }
    });

    nextBtn.addClickListener(_ -> {
      int size = Optional.ofNullable(searchBar.getPageSize()).orElse(25);
      int maxPage = Math.max(1, (int) Math.ceil((double) totalCount / size));
      if (currentPage < maxPage) {
        currentPage++;
        refreshPageInfo();
        safeRefresh();
      }
    });
  }

  private void addShortCuts() {
    UI.getCurrent().addShortcutListener(_ -> {
      if (!grid.getSelectedItems().isEmpty()) {
        bulkBar.confirmBulkDeleteSelected();
      }
    }, Key.DELETE);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    try (var _ = withRefreshGuard(false)) {
      var keys = grid.getColumns().stream().map(Grid.Column::getKey).filter(Objects::nonNull).toList();
      var vis = columnVisibilityService.mergeWithDefaults(keys);
      grid.getColumns().forEach(c -> {
        var k = c.getKey();
        if (k != null) c.setVisible(vis.getOrDefault(k, true));
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    subscription = StoreEvents.subscribe(_ -> getUI().ifPresent(ui -> ui.access(this::safeRefresh)));
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (subscription != null) {
      try {
        subscription.close();
      } catch (Exception ignored) {
      }
      subscription = null;
    }
  }

  private void configureGrid() {
    grid.addClassName(C_GRID);

    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

    grid.setSelectionMode(Grid.SelectionMode.MULTI);

    configureColumShortCode();
    configureColumUrl();
    configureColumCreated();
    configureColumActive();
    configureColumExpires();
    configureColumActions();

    // Set headers after columns exist
    grid.getColumnByKey("shortcode").setHeader(tr(K_GRID_COL_SHORTCODE, "Shortcode"));
    grid.getColumnByKey("url").setHeader(tr(K_GRID_COL_URL, "URL"));
    grid.getColumnByKey("created").setHeader(tr(K_GRID_COL_CREATED, "Created"));
    grid.getColumnByKey("active").setHeader(tr(K_GRID_COL_ACTIVE, "Active"));
    grid.getColumnByKey("expires").setHeader(tr(K_GRID_COL_EXPIRES, "Expires"));
    grid.getColumnByKey("actions").setHeader(tr(K_GRID_COL_ACTIONS, "Actions"));

    grid.addItemDoubleClickListener(ev -> openDetailsDialog(ev.getItem()));
    grid.getElement().addEventListener("keydown", _ -> {
      grid.getSelectedItems().stream().findFirst().ifPresent(this::openDetailsDialog);
    }).setFilter("event.key === 'Enter'");

    GridContextMenu<ShortUrlMapping> menu = new GridContextMenu<>(grid);
    menu.addItem(tr(K_MENU_SHOW_DETAILS, "Show details"),
                 e -> e.getItem().ifPresent(this::openDetailsDialog));
    menu.addItem(tr(K_MENU_OPEN_URL, "Open URL"),
                 e -> e.getItem().ifPresent(m -> UI.getCurrent().getPage().open(m.originalUrl(), "_blank")));
    menu.addItem(tr(K_MENU_COPY_SHORTCODE, "Copy shortcode"),
                 e -> e.getItem().ifPresent(m -> UiActions.copyToClipboard(m.shortCode())));
    menu.addItem(tr(K_MENU_DELETE, "Delete…"),
                 e -> e.getItem().ifPresent(m -> confirmDelete(m.shortCode())));
  }

  private void configureColumActions() {
    grid.addComponentColumn(this::buildGridRowActions)
        .setHeader("") // header will be set via key in configureGrid()
        .setKey("actions")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setResizable(true);
  }

  private void configureColumExpires() {
    grid.addComponentColumn(m -> {
          var statusText = computeStatusText(m.expiresAt());
          var pill = new Span();
          pill.setText(statusText.text());
          pill.getElement().getThemeList().add("badge pill small");
          pill.getElement().getThemeList().add(statusText.theme());
          return pill;
        })
        .setHeader("") // header will be set via key in configureGrid()
        .setKey("expires")
        .setAutoWidth(true)
        .setResizable(true)
        .setFlexGrow(0);
  }

  private void configureColumActive() {
    grid.addComponentColumn(m -> {
          Icon icon = m.active() ? VaadinIcon.CHECK_CIRCLE.create() : VaadinIcon.CLOSE_CIRCLE.create();
          icon.addClassName(C_ACTIVE_ICON);
          icon.addClassName(m.active() ? "is-active" : "is-inactive");

          icon.getElement().setProperty("title",
                                        m.active()
                                            ? tr(K_ACTIVE_TOOLTIP_DEACTIVATE, "Deactivate")
                                            : tr(K_ACTIVE_TOOLTIP_ACTIVATE, "Activate"));

          icon.addClickListener(_ -> {
            boolean newValue = !m.active();
            try {
              urlShortenerClient.toggleActive(m.shortCode(), newValue);
              Notifications.statusUpdatedOK();
              safeRefresh();
            } catch (Exception ex) {
              Notifications.statusUpdatedFailed(ex);
            }
          });
          return icon;
        }).setHeader("") // header will be set via key in configureGrid()
        .setKey("active").setAutoWidth(true).setResizable(true).setSortable(true).setFlexGrow(0);
  }

  private void configureColumCreated() {
    grid.addColumn(m -> DATE_TIME_FMT.format(m.createdAt()))
        .setHeader("") // header will be set via key in configureGrid()
        .setKey("created").setAutoWidth(true).setResizable(true).setSortable(true).setFlexGrow(0);
  }

  private void configureColumUrl() {
    grid.addComponentColumn(m -> {
          final var component = UiLinks.httpAwareLink(m.originalUrl(), m.originalUrl());
          component.getElement().getClassList().add(C_URL_ANCHOR);
          return component;
        }).setHeader("") // header will be set via key in configureGrid()
        .setKey("url").setFlexGrow(1).setResizable(true);
  }

  private void configureColumShortCode() {
    grid.addComponentColumn(m -> {
          var code = new Span(m.shortCode());
          code.addClassName(C_SHORTCODE);

          var copy = new Button(new Icon(VaadinIcon.COPY));
          copy.addThemeVariants(LUMO_TERTIARY_INLINE);
          copy.getElement().setProperty("title", tr(K_COPY_SHORTURL_TOOLTIP, "Copy ShortUrl"));
          copy.addClickListener(_ -> {
            var url = SHORTCODE_BASE_URL + m.shortCode();
            UiActions.copyToClipboard(url);
            Notifications.shortCodeCopied();
          });

          var wrap = new HorizontalLayout(code, copy);
          wrap.addClassName(C_SHORTCODE_WRAP);
          wrap.setSpacing(true);
          wrap.setPadding(false);
          return wrap;
        })
        .setHeader("") // header will be set via key in configureGrid()
        .setKey("shortcode")
        .setAutoWidth(true)
        .setFrozen(true)
        .setResizable(true)
        .setFlexGrow(0);
  }

  private void openDetailsDialog(ShortUrlMapping item) {
    var dlg = new DetailsDialog(urlShortenerClient, item);
    dlg.addDeleteListener(ev -> confirmDelete(ev.shortCode));
    dlg.addOpenListener(ev -> logger().info("Open URL {}", ev.originalUrl));
    dlg.addCopyShortListener(ev -> logger().info("Copied shortcode {}", ev.shortCode));
    dlg.addCopyUrlListener(ev -> logger().info("Copied URL {}", ev.url));
    dlg.addSavedListener(_ -> safeRefresh());
    dlg.open();
  }

  private Component buildGridRowActions(ShortUrlMapping m) {
    Button delete = new Button(new Icon(VaadinIcon.TRASH));
    delete.addThemeVariants(LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    delete.addClickListener(_ -> confirmDelete(m.shortCode()));

    var details = new Button(new Icon(VaadinIcon.SEARCH));
    details.addThemeVariants(LUMO_TERTIARY_INLINE);
    details.addClickListener(_ -> openDetailsDialog(m));

    var statistics = new Button(new Icon(VaadinIcon.CHART));
    statistics.addThemeVariants(LUMO_TERTIARY_INLINE);
    statistics.getElement().setProperty("title", tr(K_STATISTICS_TOOLTIP, "View statistics"));
    statistics.addClickListener(_ ->
        statistics.getUI().ifPresent(ui ->
            ui.navigate(StatisticsDetailView.class, m.shortCode())
        )
    );

    var row = new HorizontalLayout(details, statistics, delete);
    row.addClassName(C_ROW_ACTIONS);
    row.setSpacing(true);
    return row;
  }

  private void initDataProvider() {
    dataProvider = new CallbackDataProvider<>(q -> {
      final int uiSize = Optional.ofNullable(searchBar.getPageSize()).orElse(25);
      final int pageStart = (currentPage - 1) * uiSize;

      final int vLimit = q.getLimit();
      final int vOffset = q.getOffset();

      final int effectiveLimit = (vLimit > 0) ? vLimit : uiSize;
      final int effectiveOffset = pageStart + vOffset;

      final int page = (effectiveLimit > 0) ? (effectiveOffset / effectiveLimit) + 1 : 1;
      final int size = (effectiveLimit > 0) ? effectiveLimit : uiSize;

      final UrlMappingListRequest req = searchBar.buildFilter(page, size);
      try {
        final List<ShortUrlMapping> items = urlShortenerClient.list(req);
        return items.stream();
      } catch (IOException ex) {
        logger().error("Error fetching (page={}, size={})", page, size, ex);
        Notifications.loadingFailed();
        return Stream.empty();
      }
    }, _ -> {
      try {
        final UrlMappingListRequest base = searchBar.buildFilter(null, null);
        totalCount = urlShortenerClient.listCount(base);

        final int uiSize = Optional.ofNullable(searchBar.getPageSize()).orElse(25);
        final int pageStart = (currentPage - 1) * uiSize;
        final int remaining = Math.max(0, totalCount - pageStart);
        final int pageCount = Math.min(uiSize, remaining);

        refreshPageInfo();
        return pageCount;
      } catch (IOException ex) {
        logger().error("Error counting", ex);
        totalCount = 0;
        refreshPageInfo();
        return 0;
      }
    });

    grid.setPageSize(Optional.ofNullable(searchBar.getPageSize()).orElse(25));
    grid.setDataProvider(dataProvider);
  }

  private void refreshPageInfo() {
    int size = Optional.ofNullable(searchBar.getPageSize()).orElse(25);
    int maxPage = Math.max(1, (int) Math.ceil((double) totalCount / size));
    currentPage = Math.min(Math.max(1, currentPage), maxPage);
    pageInfo.setText(tr(
        "overview.paging.info",
        "Page {0} / {1} • {2} total",
        currentPage, maxPage, totalCount
    ));

    prevBtn.setEnabled(currentPage > 1);
    nextBtn.setEnabled(currentPage < maxPage);
  }

  @Override
  public void safeRefresh() {
    if (!suppressRefresh) dataProvider.refreshAll();
  }

  private void confirmDelete(String shortCode) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr(K_DELETE_TITLE, "Confirm deletion"));
    dialog.add(new Text(tr(
        "overview.delete.question",
        "Delete short link “{0}”?",
        shortCode
    )));


    Button confirm = new Button(tr(K_COMMON_DELETE, "Delete"), _ -> {
      try {
        boolean ok = urlShortenerClient.delete(shortCode);
        dialog.close();
        if (ok) {
          Notifications.shortCodeDeleted();
          safeRefresh();
        } else {
          Notifications.shortCodeNotFound();
        }
      } catch (IOException ex) {
        Notifications.operationFailed(ex);
      }
    });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, LUMO_ERROR);

    Button cancel = new Button(tr(K_COMMON_CANCEL, "Cancel"), _ -> dialog.close());
    dialog.add(new HorizontalLayout(confirm, cancel));
    dialog.open();
  }

  public void setGridPageSize(Integer gridPageSize) {
    grid.setPageSize(Optional.ofNullable(gridPageSize).orElse(25));
  }

  public void setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
  }

  @Override
  public boolean isRefreshSuppressed() {
    return suppressRefresh;
  }

  @Override
  public void setRefreshSuppressed(boolean suppressed) {
    this.suppressRefresh = suppressed;
  }
}