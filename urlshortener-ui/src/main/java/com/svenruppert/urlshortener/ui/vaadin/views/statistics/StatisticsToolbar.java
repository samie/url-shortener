package com.svenruppert.urlshortener.ui.vaadin.views.statistics;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Toolbar for statistics-specific controls: date range and aggregation granularity.
 * Designed to be used below the SearchBar in statistics views.
 */
@CssImport("./styles/statistics-toolbar.css")
public class StatisticsToolbar
    extends Composite<HorizontalLayout>
    implements HasLogger, I18nSupport {

  private static final String C_ROOT = "statistics-toolbar";
  private static final String C_DATE_RANGE = "statistics-toolbar__date-range";
  private static final String C_GRANULARITY = "statistics-toolbar__granularity";

  private static final int DEFAULT_HOT_WINDOW_DAYS = 7;

  // i18n keys
  private static final String K_FROM_LABEL = "statistics.toolbar.from.label";
  private static final String K_TO_LABEL = "statistics.toolbar.to.label";
  private static final String K_GRANULARITY_LABEL = "statistics.toolbar.granularity.label";
  private static final String K_GRANULARITY_HOUR = "statistics.toolbar.granularity.hour";
  private static final String K_GRANULARITY_DAY = "statistics.toolbar.granularity.day";
  private static final String K_GRANULARITY_WEEK = "statistics.toolbar.granularity.week";
  private static final String K_GRANULARITY_MONTH = "statistics.toolbar.granularity.month";

  private final DatePicker fromDate = new DatePicker();
  private final DatePicker toDate = new DatePicker();
  private final Select<AggregationGranularity> granularity = new Select<>();

  private int hotWindowDays = DEFAULT_HOT_WINDOW_DAYS;
  private Consumer<Void> onFilterChange;

  public StatisticsToolbar() {
    container().addClassName(C_ROOT);
    container().setWidthFull();
    container().setSpacing(true);
    container().setAlignItems(FlexComponent.Alignment.END);

    buildComponents();
    applyI18n();
    addListeners();
    setDefaultValues();
  }

  private HorizontalLayout container() {
    return getContent();
  }

  private void buildComponents() {
    fromDate.addClassName(C_DATE_RANGE);
    fromDate.setClearButtonVisible(true);

    toDate.addClassName(C_DATE_RANGE);
    toDate.setClearButtonVisible(true);

    granularity.addClassName(C_GRANULARITY);
    granularity.setItems(AggregationGranularity.values());
    granularity.setItemLabelGenerator(this::translateGranularity);
    granularity.setEmptySelectionAllowed(false);

    container().add(fromDate, toDate, granularity);
  }

  private void applyI18n() {
    fromDate.setLabel(tr(K_FROM_LABEL, "From"));
    toDate.setLabel(tr(K_TO_LABEL, "To"));
    granularity.setLabel(tr(K_GRANULARITY_LABEL, "Granularity"));
  }

  private String translateGranularity(AggregationGranularity g) {
    return switch (g) {
      case HOUR -> tr(K_GRANULARITY_HOUR, "Hour");
      case DAY -> tr(K_GRANULARITY_DAY, "Day");
      case WEEK -> tr(K_GRANULARITY_WEEK, "Week");
      case MONTH -> tr(K_GRANULARITY_MONTH, "Month");
    };
  }

  private void addListeners() {
    fromDate.addValueChangeListener(this::handleDateChange);
    toDate.addValueChangeListener(this::handleDateChange);
    granularity.addValueChangeListener(this::handleFilterChange);
  }

  private void handleFilterChange(HasValue.ValueChangeEvent<?> event) {
    if (onFilterChange != null) {
      onFilterChange.accept(null);
    }
  }

  private void handleDateChange(HasValue.ValueChangeEvent<LocalDate> event) {
    updateHourlyAvailability();
    if (onFilterChange != null) {
      onFilterChange.accept(null);
    }
  }

  private void updateHourlyAvailability() {
    LocalDate from = fromDate.getValue();
    LocalDate to = toDate.getValue();
    LocalDate hotWindowStart = LocalDate.now().minusDays(hotWindowDays);

    // Hour granularity is only available if the entire range is within the hot window
    final boolean hourlyAvailable =
        (from == null || !from.isBefore(hotWindowStart))
            && (to == null || !to.isBefore(hotWindowStart));

    // If hourly is currently selected but not available, switch to day
    if (!hourlyAvailable && granularity.getValue() == AggregationGranularity.HOUR) {
      granularity.setValue(AggregationGranularity.DAY);
    }

    granularity.setItemEnabledProvider(
        g -> g != AggregationGranularity.HOUR || hourlyAvailable
    );
  }

  private void setDefaultValues() {
    LocalDate today = LocalDate.now();
    fromDate.setValue(today.minusDays(hotWindowDays));
    toDate.setValue(today);
    granularity.setValue(AggregationGranularity.DAY);
    updateHourlyAvailability();
  }

  public void resetToDefaults() {
    setDefaultValues();
    if (onFilterChange != null) {
      onFilterChange.accept(null);
    }
  }

  // --- Getters ---

  public LocalDate getFromDate() {
    return fromDate.getValue();
  }

  public LocalDate getToDate() {
    return toDate.getValue();
  }

  public AggregationGranularity getGranularity() {
    return granularity.getValue();
  }

  // --- Setters ---

  public void setFromDate(LocalDate date) {
    fromDate.setValue(date);
  }

  public void setToDate(LocalDate date) {
    toDate.setValue(date);
  }

  public void setGranularity(AggregationGranularity g) {
    granularity.setValue(g);
  }

  public void setHotWindowDays(int days) {
    this.hotWindowDays = days;
    updateHourlyAvailability();
  }

  public void setOnFilterChange(Consumer<Void> callback) {
    this.onFilterChange = callback;
  }

  /**
   * Hides the granularity selector (used if only date range is needed).
   */
  public void hideGranularity() {
    granularity.setVisible(false);
  }
}