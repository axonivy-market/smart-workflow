package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import java.util.List;

public record ToolMetrics(
    int totalCalls,
    int totalErrors,
    int totalNullResults,
    int successes,
    int successRate,
    int errorRate,
    List<ToolStats> tools) {

  public ToolMetrics {
    tools = List.copyOf(tools);
  }

  public int getTotalCalls() {
    return totalCalls;
  }
  public int getTotalErrors() {
    return totalErrors;
  }
  public int getTotalNullResults() {
    return totalNullResults;
  }
  public int getSuccesses() {
    return successes;
  }
  public int getSuccessRate() {
    return successRate;
  }
  public int getErrorRate() {
    return errorRate;
  }
  public List<ToolStats> getTools() {
    return tools;
  }
}
