package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

public record CaseOverview(
    int agentCount,
    long totalDurationMs,
    int totalMessages,
    long anomalyAgents,
    int totalAnomalies,
    long lengthCount,
    long distinctModels,
    double avgDurationPerAgent) {

  public int getAgentCount() {
      return agentCount;
  }
  public long getTotalDurationMs() {
    return totalDurationMs;
  }
  public int getTotalMessages() {
    return totalMessages;
  }
  public long getAnomalyAgents() {
    return anomalyAgents;
  }
  public int getTotalAnomalies() {
    return totalAnomalies;
  }
  public long getLengthCount() {
    return lengthCount;
  }
  public long getDistinctModels() {
    return distinctModels;
  }
  public double getAvgDurationPerAgent() {
    return avgDurationPerAgent;
  }
}
