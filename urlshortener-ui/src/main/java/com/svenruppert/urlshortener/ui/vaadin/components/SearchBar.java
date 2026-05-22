package com.svenruppert.urlshortener.ui.vaadin.components;



import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.urlshortener.core.urlmapping.UrlMappingListRequest;
import com.svenruppert.urlshortener.ui.vaadin.tools.HasRefreshGuard;
import com.svenruppert.urlshortener.ui.vaadin.tools.I18nSupport;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static com.vaadin.flow.data.value.ValueChangeMode.LAZY;

/**
 * Reusable search/filter bar component.
 * Can be configured to show/hide various elements depending on context.
 */
@CssImport("./styles/search-bar.css")
public class SearchBar
    extends Composite<HorizontalLayout>
    implements HasLogger, HasRefreshGuard, I18nSupport {

  public static final int VALUE_CHANGE_TIMEOUT = 400;

  // CSS classes
  private static final String C_ROOT = "search-bar";
  private static final String C_TOP = "search-bar__top";
  private static final String C_ADV = "search-bar__advanced";
  private static final String C_ADV_WRAP = "search-bar__adv-wrap";
  private static final String C_SEARCH_BLOCK = "search-bar__search-block";
  private static final String C_SORTBAR = "search-bar__sortbar";
  private static final String C_FROM_GROUP = "search-bar__from-group";
  private static final String C_TO_GROUP = "search-bar__to-group";

  private static final String C_GLOBAL = "search-bar__global";
  private static final String C_SCOPE = "search-bar__scope";
  private static final String C_PAGESIZE = "search-bar__pagesize";
  private static final String C_ACTIVE = "search-bar__active";
  private static final String C_RESET = "search-bar__reset";

  private static final String C_SORTBY = "search-bar__sortby";
  private static final String C_DIR = "search-bar__dir";

  // i18n keys
  private static final String K_SCOPE_LABEL = "overview.search.scope.label";
  private static final String K_SCOPE_URL = "overview.search.scope.url";
  private static final String K_SCOPE_SHORTCODE = "overview.search.scope.shortcode";

  private static final String K_GLOBAL_PLACEHOLDER = "overview.search.global.placeholder";

  private static final String K_CODE_LABEL = "overview.search.advanced.shortcode.label";
  private static final String K_CODE_PLACEHOLDER = "overview.search.advanced.shortcode.placeholder";

  private static final String K_URL_LABEL = "overview.search.advanced.url.label";
  private static final String K_URL_PLACEHOLDER = "overview.search.advanced.url.placeholder";

  private static final String K_PAGESIZE_LABEL = "overview.search.pagesize.label";

  private static final String K_FROM_LABEL = "overview.search.from.label";
  private static final String K_TO_LABEL = "overview.search.to.label";
  private static final String K_TIME_LABEL = "overview.search.time.label";
  private static final String K_TIME_PLACEHOLDER = "overview.search.time.placeholder";

  private static final String K_SORTBY_PLACEHOLDER = "overview.search.sortBy.placeholder";
  private static final String K_DIR_PLACEHOLDER = "overview.search.dir.placeholder";

  private static final String K_SORTBY_ITEM_CREATED = "overview.search.sortBy.createdAt";
  private static final String K_SORTBY_ITEM_SHORTCODE = "overview.search.sortBy.shortCode";
  private static final String K_SORTBY_ITEM_URL = "overview.search.sortBy.originalUrl";
  private static final String K_SORTBY_ITEM_EXPIRES = "overview.search.sortBy.expiresAt";

  private static final String K_DIR_ITEM_ASC = "overview.search.dir.asc";
  private static final String K_DIR_ITEM_DESC = "overview.search.dir.desc";

  private static final String K_ACTIVE_LABEL = "overview.search.active.label";
  private static final String K_ACTIVE_ACTIVE = "overview.search.active.active";
  private static final String K_ACTIVE_INACTIVE = "overview.search.active.inactive";
  private static final String K_ACTIVE_NOT_SET = "overview.search.active.notSet";

  private static final String K_RESET = "overview.search.reset";
  private static final String K_ADVANCED = "overview.search.advanced.title";

  // technical constants (do NOT translate these)
  private static final String SCOPE_URL = "url";
  private static final String SCOPE_SHORTCODE = "shortcode";

  // UI components
  private final TextField globalSearch = new TextField();
  private final ComboBox<String> searchScope = new ComboBox<>();
  private final TextField codePart = new TextField();
  private final TextField urlPart = new TextField();
  private final IntegerField pageSize = new IntegerField();
  private final DatePicker fromDate = new DatePicker();
  private final TimePicker fromTime = new TimePicker();
  private final DatePicker toDate = new DatePicker();
  private final TimePicker toTime = new TimePicker();
  private final ComboBox<String> sortBy = new ComboBox<>();
  private final ComboBox<String> dir = new ComboBox<>();
  private final Select<ActiveState> activeState = new Select<>();
  private final Button resetBtn = new Button(new Icon(VaadinIcon.ROTATE_LEFT));

  private Details advanced;
  private HorizontalLayout topBar;

  // Callbacks
  private Consumer<Void> onFilterChange;
  private Consumer<Integer> onPageSizeChange;
  private Consumer<Void> onReset;

  // State
  private boolean refreshSuppressed = false;

  public SearchBar() {
    container().addClassName(C_ROOT);
    container().add(buildSearchBar());

    applyI18n();
    addShortCuts();
    addListeners();
  }

  private HorizontalLayout container() {
    return this.getContent();
  }

  private void applyI18n() {
    // top bar
    globalSearch.setPlaceholder(tr(K_GLOBAL_PLACEHOLDER, "Search all…"));

    searchScope.setLabel(tr(K_SCOPE_LABEL, "Search in"));
    searchScope.setItemLabelGenerator(v -> SCOPE_SHORTCODE.equals(v)
        ? tr(K_SCOPE_SHORTCODE, "Shortcode")
        : tr(K_SCOPE_URL, "URL"));

    pageSize.setLabel(tr(K_PAGESIZE_LABEL, "Page size"));

    activeState.setLabel(tr(K_ACTIVE_LABEL, "Active state"));
    activeState.setItemLabelGenerator(state -> switch (state) {
      case ACTIVE -> tr(K_ACTIVE_ACTIVE, "Active");
      case INACTIVE -> tr(K_ACTIVE_INACTIVE, "Inactive");
      case NOT_SET -> tr(K_ACTIVE_NOT_SET, "Not set");
    });

    resetBtn.setText(tr(K_RESET, "Reset"));

    // advanced fields
    codePart.setLabel(tr(K_CODE_LABEL, "Shortcode contains"));
    codePart.setPlaceholder(tr(K_CODE_PLACEHOLDER, "e.g. ex-"));

    urlPart.setLabel(tr(K_URL_LABEL, "Original URL contains"));
    urlPart.setPlaceholder(tr(K_URL_PLACEHOLDER, "e.g. docs"));

    fromDate.setLabel(tr(K_FROM_LABEL, "From (local)"));
    toDate.setLabel(tr(K_TO_LABEL, "To (local)"));
    fromTime.setLabel(tr(K_TIME_LABEL, "Time"));
    toTime.setLabel(tr(K_TIME_LABEL, "Time"));

    fromTime.setPlaceholder(tr(K_TIME_PLACEHOLDER, "hh:mm"));
    toTime.setPlaceholder(tr(K_TIME_PLACEHOLDER, "hh:mm"));

    sortBy.setPlaceholder(tr(K_SORTBY_PLACEHOLDER, "Sort by"));
    dir.setPlaceholder(tr(K_DIR_PLACEHOLDER, "Direction"));

    sortBy.setItemLabelGenerator(v -> switch (v) {
      case "createdAt" -> tr(K_SORTBY_ITEM_CREATED, "Created");
      case "shortCode" -> tr(K_SORTBY_ITEM_SHORTCODE, "Shortcode");
      case "originalUrl" -> tr(K_SORTBY_ITEM_URL, "Original URL");
      case "expiresAt" -> tr(K_SORTBY_ITEM_EXPIRES, "Expires");
      default -> v;
    });

    dir.setItemLabelGenerator(v -> "asc".equals(v)
        ? tr(K_DIR_ITEM_ASC, "Ascending")
        : tr(K_DIR_ITEM_DESC, "Descending"));

    if (advanced != null) {
      advanced.setSummaryText(tr(K_ADVANCED, "Advanced filters"));
    }
  }

  private Component buildSearchBar() {
    // --- Top bar ---
    globalSearch.addClassName(C_GLOBAL);
    globalSearch.setClearButtonVisible(true);
    globalSearch.setValueChangeMode(LAZY);
    globalSearch.setValueChangeTimeout(VALUE_CHANGE_TIMEOUT);

    searchScope.addClassName(C_SCOPE);
    searchScope.setItems(SCOPE_URL, SCOPE_SHORTCODE);
    searchScope.setValue(SCOPE_URL);

    pageSize.addClassName(C_PAGESIZE);
    pageSize.setMin(1);
    pageSize.setMax(500);
    pageSize.setStepButtonsVisible(true);

    activeState.addClassName(C_ACTIVE);
    activeState.setItems(ActiveState.values());
    activeState.setEmptySelectionAllowed(false);
    activeState.setValue(ActiveState.NOT_SET);

    resetBtn.addClassName(C_RESET);

    topBar = new HorizontalLayout(globalSearch, searchScope, pageSize, activeState, resetBtn);
    topBar.addClassName(C_TOP);
    topBar.setWidthFull();
    topBar.setSpacing(true);
    topBar.setAlignItems(FlexComponent.Alignment.END);

    // --- Advanced block ---
    codePart.setValueChangeMode(LAZY);
    codePart.setValueChangeTimeout(VALUE_CHANGE_TIMEOUT);
    codePart.addValueChangeListener(_ -> fireFilterChange());

    urlPart.setValueChangeMode(LAZY);
    urlPart.setValueChangeTimeout(VALUE_CHANGE_TIMEOUT);
    urlPart.addValueChangeListener(_ -> fireFilterChange());

    sortBy.addClassName(C_SORTBY);
    sortBy.setItems("createdAt", "shortCode", "originalUrl", "expiresAt");
    sortBy.setLabel(null);

    dir.addClassName(C_DIR);
    dir.setItems("asc", "desc");
    dir.setLabel(null);

    fromDate.setClearButtonVisible(true);
    toDate.setClearButtonVisible(true);

    fromTime.setStep(Duration.ofMinutes(15));
    toTime.setStep(Duration.ofMinutes(15));

    var fromGroup = new HorizontalLayout(fromDate, fromTime);
    fromGroup.addClassName(C_FROM_GROUP);
    fromGroup.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

    var toGroup = new HorizontalLayout(toDate, toTime);
    toGroup.addClassName(C_TO_GROUP);
    toGroup.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

    FormLayout searchBlock = new FormLayout();
    searchBlock.addClassName(C_SEARCH_BLOCK);
    searchBlock.setWidthFull();
    searchBlock.add(codePart, urlPart, new HorizontalLayout()); // placeholder
    searchBlock.add(fromGroup, toGroup);
    searchBlock.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("32rem", 2),
        new FormLayout.ResponsiveStep("56rem", 3)
    );

    HorizontalLayout sortToolbar = new HorizontalLayout(sortBy, dir);
    sortToolbar.addClassName(C_SORTBAR);
    sortToolbar.setAlignItems(FlexComponent.Alignment.END);

    HorizontalLayout advHeader = new HorizontalLayout(searchBlock, sortToolbar);
    advHeader.addClassName(C_ADV_WRAP);
    advHeader.setWidthFull();
    advHeader.setSpacing(true);
    advHeader.setAlignItems(FlexComponent.Alignment.START);
    advHeader.expand(searchBlock);
    advHeader.setVerticalComponentAlignment(FlexComponent.Alignment.END, sortToolbar);

    advanced = new Details(tr(K_ADVANCED, "Advanced filters"), advHeader);
    advanced.addClassName(C_ADV);
    advanced.setOpened(false);
    advanced.getElement().getThemeList().add("filled");

    setSimpleSearchEnabled(!advanced.isOpened());

    var searchBar = new VerticalLayout();
    searchBar.setPadding(false);
    searchBar.setSpacing(true);
    searchBar.add(topBar, advanced);

    return searchBar;
  }

  private void addListeners() {
    activeState.addValueChangeListener(_ -> fireFilterChange());

    globalSearch.addValueChangeListener(e -> {
      var v = Optional.ofNullable(e.getValue()).orElse("");
      if (SCOPE_SHORTCODE.equals(searchScope.getValue())) {
        codePart.setValue(v);
        urlPart.clear();
      } else {
        urlPart.setValue(v);
        codePart.clear();
      }
      fireFilterChange();
    });

    searchScope.addValueChangeListener(_ -> {
      var v = Optional.ofNullable(globalSearch.getValue()).orElse("");
      if (SCOPE_SHORTCODE.equals(searchScope.getValue())) {
        codePart.setValue(v);
        urlPart.clear();
      } else {
        urlPart.setValue(v);
        codePart.clear();
      }
      fireFilterChange();
    });

    pageSize.addValueChangeListener(e -> {
      if (onPageSizeChange != null) {
        onPageSizeChange.accept(e.getValue());
      }
      fireFilterChange();
    });

    advanced.addOpenedChangeListener(ev -> {
      boolean nowClosed = !ev.isOpened();
      if (nowClosed) {
        applyAdvancedToSimpleAndReset();
      } else {
        setSimpleSearchEnabled(false);
      }
    });

    resetBtn.addClickListener(_ -> {
      try (var _ = withRefreshGuard(true)) {
        resetElements();
        if (onReset != null) {
          onReset.accept(null);
        }
      } catch (Exception e) {
        logger().warn("resetBtn.addClickListener failed {}", e.getMessage());
      }
    });
  }

  private void addShortCuts() {
    var current = UI.getCurrent();
    if (current == null) return;

    current.addShortcutListener(_ -> {
      if (globalSearch.isEnabled()) globalSearch.focus();
    }, Key.KEY_K, KeyModifier.CONTROL);

    current.addShortcutListener(_ -> {
      if (globalSearch.isEnabled()) globalSearch.focus();
    }, Key.KEY_K, KeyModifier.META);
  }

  private void setSimpleSearchEnabled(boolean enabled) {
    globalSearch.setEnabled(enabled);
    searchScope.setEnabled(enabled);
    resetBtn.setEnabled(true);
  }

  private void applyAdvancedToSimpleAndReset() {
    String code = Optional.ofNullable(codePart.getValue()).orElse("").trim();
    String url = Optional.ofNullable(urlPart.getValue()).orElse("").trim();

    final boolean hasCode = !code.isBlank();
    final boolean hasUrl = !url.isBlank();
    final String winnerValue = hasCode ? code : (hasUrl ? url : "");
    final String winnerScope = hasCode ? SCOPE_SHORTCODE : SCOPE_URL;

    try (var _ = withRefreshGuard(true)) {
      codePart.clear();
      urlPart.clear();
      fromDate.clear();
      fromTime.clear();
      toDate.clear();
      toTime.clear();

      sortBy.clear();
      dir.clear();
      sortBy.setValue("createdAt");
      dir.setValue("desc");

      searchScope.setValue(winnerScope);
      if (!winnerValue.isBlank()) globalSearch.setValue(winnerValue);
      else globalSearch.clear();

      setSimpleSearchEnabled(true);
      globalSearch.focus();
    } catch (Exception e) {
      logger().warn("applyAdvancedToSimpleAndReset failed {}", e.getMessage());
    }
  }

  private void fireFilterChange() {
    if (!refreshSuppressed && onFilterChange != null) {
      onFilterChange.accept(null);
    }
  }

  // ============================================================================
  // Public API - Callbacks
  // ============================================================================

  public void setOnFilterChange(Consumer<Void> callback) {
    this.onFilterChange = callback;
  }

  public void setOnPageSizeChange(Consumer<Integer> callback) {
    this.onPageSizeChange = callback;
  }

  public void setOnReset(Consumer<Void> callback) {
    this.onReset = callback;
  }

  // ============================================================================
  // Public API - Hide/Show components
  // ============================================================================

  /**
   * Hides the active state filter (used in statistics view).
   */
  public void hideActiveState() {
    activeState.setVisible(false);
  }

  /**
   * Hides the search scope selector.
   */
  public void hideSearchScope() {
    searchScope.setVisible(false);
  }

  /**
   * Hides the page size field.
   */
  public void hidePageSize() {
    pageSize.setVisible(false);
  }

  /**
   * Hides the advanced filters section entirely.
   */
  public void hideAdvanced() {
    advanced.setVisible(false);
  }

  /**
   * Hides the global search field.
   */
  public void hideGlobalSearch() {
    globalSearch.setVisible(false);
  }

  // ============================================================================
  // Public API - Build filter request
  // ============================================================================

  public UrlMappingListRequest buildFilter(Integer page, Integer size) {
    UrlMappingListRequest.Builder b = UrlMappingListRequest.builder();

    ActiveState activeStateValue = activeState.getValue();
    if (activeStateValue != null && activeStateValue.isSet()) {
      var v = activeStateValue.toBoolean();
      b.active(Boolean.TRUE.equals(v));
    }

    if (codePart.getValue() != null && !codePart.getValue().isBlank()) b.codePart(codePart.getValue());
    if (urlPart.getValue() != null && !urlPart.getValue().isBlank()) b.urlPart(urlPart.getValue());

    if (fromDate.getValue() != null && fromTime.getValue() != null) {
      var zdt = ZonedDateTime.of(fromDate.getValue(), fromTime.getValue(), ZoneId.systemDefault());
      b.from(zdt.toInstant());
    } else if (fromDate.getValue() != null) {
      var zdt = fromDate.getValue().atStartOfDay(ZoneId.systemDefault());
      b.from(zdt.toInstant());
    }

    if (toDate.getValue() != null && toTime.getValue() != null) {
      var zdt = ZonedDateTime.of(toDate.getValue(), toTime.getValue(), ZoneId.systemDefault());
      b.to(zdt.toInstant());
    } else if (toDate.getValue() != null) {
      var zdt = toDate.getValue().atTime(23, 59).atZone(ZoneId.systemDefault());
      b.to(zdt.toInstant());
    }

    if (sortBy.getValue() != null && !sortBy.getValue().isBlank()) b.sort(sortBy.getValue());
    if (dir.getValue() != null && !dir.getValue().isBlank()) b.dir(dir.getValue());

    if (page != null && size != null) b.page(page).size(size);

    return b.build();
  }

  // ============================================================================
  // Public API - Getters/Setters
  // ============================================================================

  public void resetElements() {
    globalSearch.clear();
    codePart.clear();
    urlPart.clear();
    fromDate.clear();
    fromTime.clear();
    toDate.clear();
    toTime.clear();
    sortBy.clear();
    dir.clear();
    pageSize.setValue(25);
    sortBy.setValue("createdAt");
    dir.setValue("desc");

    searchScope.setValue(SCOPE_URL);
    advanced.setOpened(false);
    setSimpleSearchEnabled(true);
    globalSearch.focus();
    activeState.setValue(ActiveState.NOT_SET);
  }

  public Integer getPageSize() {
    return pageSize.getValue();
  }

  public void setPageSize(int value) {
    pageSize.setValue(value);
  }

  public void setSortBy(String value) {
    sortBy.setValue(value);
  }

  public void setDirValue(String value) {
    dir.setValue(value);
  }

  public String getSearchText() {
    return Optional.ofNullable(globalSearch.getValue()).orElse("").trim();
  }

  public String getCodePart() {
    return Optional.ofNullable(codePart.getValue()).orElse("").trim();
  }

  public String getUrlPart() {
    return Optional.ofNullable(urlPart.getValue()).orElse("").trim();
  }

  public LocalDate getFromDate() {
    return fromDate.getValue();
  }

  public void setFromDate(LocalDate date) {
    fromDate.setValue(date);
  }

  public LocalDate getToDate() {
    return toDate.getValue();
  }

  public void setToDate(LocalDate date) {
    toDate.setValue(date);
  }

  // ============================================================================
  // HasRefreshGuard implementation
  // ============================================================================

  @Override
  public boolean isRefreshSuppressed() {
    return refreshSuppressed;
  }

  @Override
  public void setRefreshSuppressed(boolean suppressed) {
    this.refreshSuppressed = suppressed;
  }

  @Override
  public void safeRefresh() {
    fireFilterChange();
  }
}