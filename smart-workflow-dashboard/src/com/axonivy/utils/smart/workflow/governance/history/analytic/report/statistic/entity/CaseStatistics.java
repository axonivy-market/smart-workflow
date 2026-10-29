package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import java.util.List;
import java.util.Objects;

public record CaseStatistics(
    String caseId,
    String caseName,
    String processName,
    CaseOverview overview,
    TokenMetrics tokenMetrics,
    ToolMetrics toolMetrics,
    Observations observations,
    List<AgentStats> agentStats) {

  public CaseStatistics {
    Objects.requireNonNull(overview);
    Objects.requireNonNull(tokenMetrics);
    Objects.requireNonNull(toolMetrics);
    Objects.requireNonNull(observations);
    agentStats = List.copyOf(agentStats);
  }

  public String getCaseId() {
    return caseId;
  }
  public String getCaseName() {
    return caseName;
  }
  public String getProcessName() {
    return processName;
  }
  public CaseOverview getOverview() {
    return overview;
  }
  public TokenMetrics getTokenMetrics() {
    return tokenMetrics;
  }
  public ToolMetrics getToolMetrics() {
    return toolMetrics;
  }
  public List<AgentStats> getAgentStats() {
    return agentStats;
  }
  public Observations getObservations() {
    return observations;
  }
}
