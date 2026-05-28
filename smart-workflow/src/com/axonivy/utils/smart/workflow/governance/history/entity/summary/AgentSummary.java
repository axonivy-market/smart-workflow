package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

import java.util.List;

public class AgentSummary {

  private String agentId;
  private String agentName;
  private String processName;
  private int messageCount;
  private int toolCallCount;
  private int totalTokens;
  private long durationMs;
  private String finishReason;
  private String model;
  private List<ToolSummary> toolSummaries;
  private List<GuardrailSummary> guardrailSummaries;
  private TokenUsageSummary tokenUsageSummary;
  private AnomalyReport anomalyReport;

  public AgentSummary() {}

  public AgentSummary(String agentId, String agentName, String processName,
      int messageCount, int toolCallCount, int totalTokens,
      long durationMs, String finishReason, String model, List<ToolSummary> toolSummaries,
      List<GuardrailSummary> guardrailSummaries, TokenUsageSummary tokenUsageSummary,
      AnomalyReport anomalyReport) {
    this.agentId = agentId;
    this.agentName = agentName;
    this.processName = processName;
    this.messageCount = messageCount;
    this.toolCallCount = toolCallCount;
    this.totalTokens = totalTokens;
    this.durationMs = durationMs;
    this.finishReason = finishReason;
    this.model = model;
    this.toolSummaries = toolSummaries;
    this.guardrailSummaries = guardrailSummaries;
    this.tokenUsageSummary = tokenUsageSummary;
    this.anomalyReport = anomalyReport;
  }

  public String getAgentId() { return agentId; }
  public void setAgentId(String agentId) { this.agentId = agentId; }

  public String getAgentName() { return agentName; }
  public void setAgentName(String agentName) { this.agentName = agentName; }

  public String getProcessName() { return processName; }
  public void setProcessName(String processName) { this.processName = processName; }

  public int getMessageCount() { return messageCount; }
  public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

  public int getToolCallCount() { return toolCallCount; }
  public void setToolCallCount(int toolCallCount) { this.toolCallCount = toolCallCount; }

  public int getTotalTokens() { return totalTokens; }
  public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

  public long getDurationMs() { return durationMs; }
  public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

  public String getFinishReason() { return finishReason; }
  public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }

  public List<ToolSummary> getToolSummaries() { return toolSummaries; }
  public void setToolSummaries(List<ToolSummary> toolSummaries) { this.toolSummaries = toolSummaries; }

  public List<GuardrailSummary> getGuardrailSummaries() { return guardrailSummaries; }
  public void setGuardrailSummaries(List<GuardrailSummary> guardrailSummaries) { this.guardrailSummaries = guardrailSummaries; }

  public TokenUsageSummary getTokenUsageSummary() { return tokenUsageSummary; }
  public void setTokenUsageSummary(TokenUsageSummary tokenUsageSummary) { this.tokenUsageSummary = tokenUsageSummary; }

  public AnomalyReport getAnomalyReport() { return anomalyReport; }
  public void setAnomalyReport(AnomalyReport anomalyReport) { this.anomalyReport = anomalyReport; }
}
