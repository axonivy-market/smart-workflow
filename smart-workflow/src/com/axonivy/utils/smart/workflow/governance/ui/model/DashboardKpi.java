package com.axonivy.utils.smart.workflow.governance.ui.model;

public class DashboardKpi {

  public static final String NO_DATA = "-";

  private final int totalSessions;
  private final long totalTokens;
  private final long avgResponseMs;
  private final String topModel;

  public DashboardKpi(int totalSessions, long totalTokens, long avgResponseMs, String topModel) {
    this.totalSessions = totalSessions;
    this.totalTokens = totalTokens;
    this.avgResponseMs = avgResponseMs;
    this.topModel = topModel;
  }

  public static DashboardKpi empty() {
    return new DashboardKpi(0, 0L, 0L, NO_DATA);
  }

  public int getTotalSessions() { return totalSessions; }
  public long getTotalTokens() { return totalTokens; }
  public long getAvgResponseMs() { return avgResponseMs; }
  public String getTopModel() { return topModel; }

  public String getFormattedTotalTokens() {
    return String.format("%,d", totalTokens);
  }
}
