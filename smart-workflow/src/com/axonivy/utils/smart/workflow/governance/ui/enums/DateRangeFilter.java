package com.axonivy.utils.smart.workflow.governance.ui.enums;

import java.time.LocalDate;

public enum DateRangeFilter {

  TODAY("Today", 1),
  LAST_7_DAYS("Last 7 days", 7),
  LAST_30_DAYS("Last 30 days", 30),
  LAST_90_DAYS("Last 90 days", 90),
  ALL("All time", -1);

  private final String displayName;
  private final int days;

  DateRangeFilter(String displayName, int days) {
    this.displayName = displayName;
    this.days = days;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getDays() {
    return days;
  }

  public LocalDate toDateFrom() {
    if (days < 0) {
      return null;
    }
    return LocalDate.now().minusDays(days - 1);
  }

  public LocalDate toDateTo() {
    if (days < 0) {
      return null;
    }
    return LocalDate.now();
  }
}
