package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

public record AgentStats(
    int index,
    String displayName,
    AgentSummary summary,
    double tokensPerMsg,
    double throughputTokensPerSec,
    int tokenSharePct,
    int timeSharePct,
    String grade) {

  public int getIndex() { return index; }
  public String getDisplayName() { return displayName; }
  public AgentSummary getSummary() { return summary; }
  public double getTokensPerMsg() { return tokensPerMsg; }
  public double getThroughputTokensPerSec() { return throughputTokensPerSec; }
  public int getTokenSharePct() { return tokenSharePct; }
  public int getTimeSharePct() { return timeSharePct; }
  public String getGrade() { return grade; }
}
