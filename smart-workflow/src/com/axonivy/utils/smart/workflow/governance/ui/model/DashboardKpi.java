package com.axonivy.utils.smart.workflow.governance.ui.model;

public class DashboardKpi {

  private final int totalSessions;
  private final long totalTokens;
  private final long avgResponseMs;
  private final String topModel;
  private final int topModelPct;
  private final int sessionsTrend;
  private final int tokensTrend;
  private final int responseMsTrend;

  public DashboardKpi(int totalSessions, long totalTokens, long avgResponseMs,
      String topModel, int topModelPct,
      int sessionsTrend, int tokensTrend, int responseMsTrend) {
    this.totalSessions = totalSessions;
    this.totalTokens = totalTokens;
    this.avgResponseMs = avgResponseMs;
    this.topModel = topModel;
    this.topModelPct = topModelPct;
    this.sessionsTrend = sessionsTrend;
    this.tokensTrend = tokensTrend;
    this.responseMsTrend = responseMsTrend;
  }

  public static DashboardKpi empty() {
    return new DashboardKpi(0, 0L, 0L, "\u2014", 0, 0, 0, 0);
  }

  public int getTotalSessions() { return totalSessions; }
  public long getTotalTokens() { return totalTokens; }
  public long getAvgResponseMs() { return avgResponseMs; }
  public String getTopModel() { return topModel; }
  public int getTopModelPct() { return topModelPct; }
  public int getSessionsTrend() { return sessionsTrend; }
  public int getTokensTrend() { return tokensTrend; }
  public int getResponseMsTrend() { return responseMsTrend; }

  public String getFormattedTotalTokens() {
    return String.format("%,d", totalTokens);
  }
}
