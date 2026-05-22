package com.svenruppert.urlshortener.ui.vaadin.views.statistics;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.client.StatisticsClient;
import com.svenruppert.urlshortener.client.URLShortenerClient;
import com.svenruppert.urlshortener.core.statistics.HourlyStatisticsResponse;
import com.svenruppert.urlshortener.core.statistics.StatisticsCountResponse;
import com.svenruppert.urlshortener.core.statistics.StatisticsTimelineResponse;
import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;
import com.svenruppert.urlshortener.ui.vaadin.MainLayout;
import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.VisibleFor;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.svenruppert.urlshortener.ui.vaadin.tools.StatisticsClientFactory;
import com.svenruppert.urlshortener.ui.vaadin.tools.UrlShortenerClientFactory;
import com.svenruppert.urlshortener.ui.vaadin.views.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Detail view showing statistics for a specific shortcode.
 * Uses StatisticsToolbar for date range and granularity controls.
 */
@PageTitle("Statistics Detail")
@Route(value = StatisticsDetailView.PATH, layout = MainLayout.class)
@VisibleFor(AppRole.USER)
@CssImport("./styles/statistics-detail-view.css")
public class StatisticsDetailView
    extends VerticalLayout
    implements HasUrlParameter<String>, HasLogger, I18nSupport {

  public static final String PATH = "statisticDetails";

  private static final String C_ROOT = "statistics-detail-view";
  private static final String C_HEADER = "statistics-detail-view__header";
  private static final String C_BACK_BTN = "statistics-detail-view__back";
  private static final String C_INFO = "statistics-detail-view__info";
  private static final String C_GRID = "statistics-detail-view__grid";
  private static final String C_TOTAL = "statistics-detail-view__total";

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // i18n keys
  private static final String K_TITLE = "statistics.detail.title";
  private static final String K_BACK = "statistics.detail.back";
  private static final String K_SHORTCODE = "statistics.detail.shortcode";
  private static final String K_URL = "statistics.detail.url";
  private static final String K_TOTAL = "statistics.detail.total";
  private static final String K_COL_PERIOD = "statistics.detail.column.period";
  private static final String K_COL_COUNT = "statistics.detail.column.count";
  private static final String K_NOT_FOUND = "statistics.detail.notFound";

  private final URLShortenerClient urlShortenerClient = UrlShortenerClientFactory.newInstance();
  private final StatisticsClient statisticsClient = StatisticsClientFactory.newInstance();

  private final StatisticsToolbar statisticsToolbar = new StatisticsToolbar();
  private final Grid<AggregateRow> grid = new Grid<>();

  private final H2 title = new H2();
  private final Span shortCodeLabel = new Span();
  private final Anchor urlAnchor = new Anchor();
  private final Span totalCount = new Span();

  private String shortCode;

  public StatisticsDetailView() {
    addClassName(C_ROOT);
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    statisticsToolbar.setOnFilterChange(_ -> loadData());

    configureGrid();

    add(buildHeader(), statisticsToolbar, buildInfoBar(), grid);
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
    if (parameter == null || parameter.isBlank()) {
      Notifications.errorKey(K_NOT_FOUND, "No shortcode specified");
      event.forwardTo(StatisticsView.class);
      return;
    }

    this.shortCode = parameter;
    loadMappingInfo();
    loadData();
  }

  private HorizontalLayout buildHeader() {
    Button backBtn = new Button(new Icon(VaadinIcon.ARROW_LEFT));
    backBtn.addClassName(C_BACK_BTN);
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backBtn.setText(tr(K_BACK, "Back"));
    backBtn.addClickListener(_ ->
        backBtn.getUI().ifPresent(ui -> ui.navigate(StatisticsView.class))
    );

    title.setText(tr(K_TITLE, "Statistics Detail"));

    HorizontalLayout header = new HorizontalLayout(backBtn, title);
    header.addClassName(C_HEADER);
    header.setAlignItems(Alignment.CENTER);
    header.setSpacing(true);

    return header;
  }

  private HorizontalLayout buildInfoBar() {
    Span codePrefix = new Span(tr(K_SHORTCODE, "Shortcode") + ": ");
    shortCodeLabel.getStyle().set("font-weight", "bold");

    Span urlPrefix = new Span(" | " + tr(K_URL, "URL") + ": ");
    urlAnchor.setTarget("_blank");

    Span totalPrefix = new Span(" | " + tr(K_TOTAL, "Total") + ": ");
    totalCount.getStyle().set("font-weight", "bold");
    totalCount.addClassName(C_TOTAL);

    HorizontalLayout info = new HorizontalLayout(
        codePrefix, shortCodeLabel,
        urlPrefix, urlAnchor,
        totalPrefix, totalCount
    );
    info.addClassName(C_INFO);
    info.setAlignItems(Alignment.CENTER);
    info.setSpacing(false);
    info.getStyle().set("gap", "4px");

    return info;
  }

  private void configureGrid() {
    grid.addClassName(C_GRID);
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSelectionMode(Grid.SelectionMode.NONE);

    grid.addColumn(AggregateRow::periodLabel)
        .setHeader(tr(K_COL_PERIOD, "Period"))
        .setKey("period")
        .setAutoWidth(true)
        .setSortable(true);

    grid.addColumn(AggregateRow::count)
        .setHeader(tr(K_COL_COUNT, "Redirects"))
        .setKey("count")
        .setAutoWidth(true)
        .setSortable(true);
  }

  private void loadMappingInfo() {
    try {
      Optional<ShortUrlMapping> mapping = urlShortenerClient.getMapping(shortCode);
      if (mapping.isPresent()) {
        shortCodeLabel.setText(shortCode);
        urlAnchor.setText(mapping.get().originalUrl());
        urlAnchor.setHref(mapping.get().originalUrl());
      } else {
        shortCodeLabel.setText(shortCode);
        urlAnchor.setText("(not found)");
      }
    } catch (IOException ex) {
      logger().error("Failed to load mapping info for {}", shortCode, ex);
      shortCodeLabel.setText(shortCode);
      urlAnchor.setText("(error)");
    }
  }

  private void loadData() {
    if (shortCode == null) return;

    LocalDate from = statisticsToolbar.getFromDate();
    LocalDate to = statisticsToolbar.getToDate();
    AggregationGranularity granularity = statisticsToolbar.getGranularity();

    // Load total count for the selected period
    loadTotalCount(from, to);

    // Load aggregated data based on granularity
    List<AggregateRow> rows = switch (granularity) {
      case HOUR -> loadHourlyData(from, to);
      case DAY -> loadDailyData(from, to);
      case WEEK -> loadWeeklyData(from, to);
      case MONTH -> loadMonthlyData(from, to);
    };

    grid.setItems(rows);
  }

  private void loadTotalCount(LocalDate from, LocalDate to) {
    try {
      StatisticsCountResponse response;
      if (from != null && to != null) {
        response = statisticsClient.getCountForDateRange(shortCode, from, to);
      } else {
        response = statisticsClient.getTotalCount(shortCode);
      }
      totalCount.setText(response != null ? String.valueOf(response.count()) : "0");
    } catch (IOException ex) {
      logger().error("Failed to load total count for {}", shortCode, ex);
      totalCount.setText("?");
    }
  }

  private List<AggregateRow> loadHourlyData(LocalDate from, LocalDate to) {
    List<AggregateRow> rows = new ArrayList<>();

    try {
      LocalDate current = from != null ? from : LocalDate.now().minusDays(7);
      LocalDate end = to != null ? to : LocalDate.now();

      while (!current.isAfter(end)) {
        Optional<HourlyStatisticsResponse> hourlyOpt =
            statisticsClient.getHourlyStatistics(shortCode, current);

        if (hourlyOpt.isPresent()) {
          HourlyStatisticsResponse hourly = hourlyOpt.get();
          long[] counts = hourly.hourlyCounts();

          for (int hour = 0; hour < 24; hour++) {
            if (counts[hour] > 0) {
              String label = current.format(DATE_FMT) + " " + String.format("%02d:00", hour);
              rows.add(new AggregateRow(label, counts[hour]));
            }
          }
        }
        current = current.plusDays(1);
      }
    } catch (IOException ex) {
      logger().error("Failed to load hourly data for {}", shortCode, ex);
    }

    return rows;
  }

  private List<AggregateRow> loadDailyData(LocalDate from, LocalDate to) {
    List<AggregateRow> rows = new ArrayList<>();

    try {
      StatisticsTimelineResponse timeline = statisticsClient.getTimeline(
          shortCode,
          from != null ? from : LocalDate.now().minusDays(30),
          to != null ? to : LocalDate.now()
      );

      if (timeline != null && timeline.dailyCounts() != null) {
        for (var dailyCount : timeline.dailyCounts()) {
          String label = dailyCount.date().format(DATE_FMT);
          rows.add(new AggregateRow(label, dailyCount.count()));
        }
      }
    } catch (IOException ex) {
      logger().error("Failed to load daily data for {}", shortCode, ex);
    }

    return rows;
  }

  private List<AggregateRow> loadWeeklyData(LocalDate from, LocalDate to) {
    List<AggregateRow> dailyRows = loadDailyData(from, to);

    Map<String, Long> weeklyAggregates = new LinkedHashMap<>();
    WeekFields weekFields = WeekFields.of(Locale.getDefault());

    for (AggregateRow row : dailyRows) {
      LocalDate date = LocalDate.parse(row.periodLabel(), DATE_FMT);
      int year = date.getYear();
      int week = date.get(weekFields.weekOfWeekBasedYear());
      String weekLabel = year + "-W" + String.format("%02d", week);

      weeklyAggregates.merge(weekLabel, row.count(), Long::sum);
    }

    return weeklyAggregates.entrySet().stream()
        .map(e -> new AggregateRow(e.getKey(), e.getValue()))
        .toList();
  }

  private List<AggregateRow> loadMonthlyData(LocalDate from, LocalDate to) {
    List<AggregateRow> dailyRows = loadDailyData(from, to);

    Map<String, Long> monthlyAggregates = new LinkedHashMap<>();

    for (AggregateRow row : dailyRows) {
      LocalDate date = LocalDate.parse(row.periodLabel(), DATE_FMT);
      String monthLabel = date.getYear() + "-" + String.format("%02d", date.getMonthValue());

      monthlyAggregates.merge(monthLabel, row.count(), Long::sum);
    }

    return monthlyAggregates.entrySet().stream()
        .map(e -> new AggregateRow(e.getKey(), e.getValue()))
        .toList();
  }

  /**
   * Record representing a single row in the aggregates table.
   */
  public record AggregateRow(String periodLabel, long count) {
  }
}