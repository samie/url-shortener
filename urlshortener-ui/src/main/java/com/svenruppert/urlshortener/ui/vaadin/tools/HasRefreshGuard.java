package com.svenruppert.urlshortener.ui.vaadin.tools;



public interface HasRefreshGuard {

  /**
   * Indicates whether the refresh operation is currently suppressed.
   * When refresh is suppressed, methods relying on refresh mechanisms will not perform updates
   * to the view or data. This method can be used to check the suppression state before
   * executing refresh-related operations.
   *
   * @return true if refresh operations are suppressed; false otherwise.
   */
  boolean isRefreshSuppressed();

  /**
   * Sets whether the refresh operation is currently suppressed.
   * When refresh is suppressed, methods relying on refresh mechanisms
   * (e.g., {@link #safeRefresh()}) will not perform any updates to the view or data.
   *
   * This method is typically used in conjunction with bulk updates or operations
   * where multiple changes occur within a short time frame, and a refresh operation
   * would be redundant or unnecessary.
   *
   * @param suppressed a boolean value indicating whether refresh should be suppressed:
   *                   - true to suppress refresh operations,
   *                   - false to re-enable them.
   */
  void setRefreshSuppressed(boolean suppressed);

  /**
   * Refreshes the view or grid data in a safe manner, taking into account whether the refresh
   * is currently suppressed. If refresh suppression is enabled, the method does not perform any
   * operations. Otherwise, it ensures that all data is refreshed and updated in the UI.
   *
   * This method should be used in scenarios where a refresh operation is necessary but must be
   * guarded against unintended executions, such as during bulk updates or when changes in state
   * occur that might otherwise cause redundant refresh operations.
   */
  void safeRefresh();

  /**
   * Guards the execution of a sequence by suppressing refresh operations temporarily
   * and optionally performing a refresh after execution. This method ensures that
   * nested operations or bulk updates can be executed without triggering redundant
   * refreshes, improving performance and avoiding inconsistent states.
   *
   * @param refreshAfter a boolean flag indicating whether a refresh operation should
   *                     be performed after the execution. If true, the {@link #safeRefresh()}
   *                     method will be invoked at the end of the operation.
   * @return an {@link AutoCloseable} instance that, when closed, restores the refresh
   *         suppression state to its previous value and optionally triggers a refresh
   *         if specified.
   */
  default AutoCloseable withRefreshGuard(boolean refreshAfter) {
    boolean prev = isRefreshSuppressed();
    setRefreshSuppressed(true);

    return () -> {
      setRefreshSuppressed(prev);
      if (refreshAfter) {
        safeRefresh();
      }
    };
  }
}