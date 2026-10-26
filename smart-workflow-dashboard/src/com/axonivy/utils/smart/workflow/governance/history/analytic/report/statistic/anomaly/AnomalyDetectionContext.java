package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

public class AnomalyDetectionContext {

  private final AgentSummary agentSummary;
  private final Supplier<List<ToolExecution>> toolExecutionsSource;
  private List<ToolExecution> toolExecutions;
  private boolean toolExecutionsLoaded;

  public AnomalyDetectionContext(AgentSummary agentSummary, Supplier<List<ToolExecution>> toolExecutionsSource) {
    this.agentSummary = Objects.requireNonNull(agentSummary, "agentSummary");
    this.toolExecutionsSource = Objects.requireNonNull(toolExecutionsSource, "toolExecutionsSource");
  }

  public int totalTokens() {
    return agentSummary.getTotalTokens();
  }

  public long durationMs() {
    return agentSummary.getDurationMs();
  }

  public String finishReason() {
    return agentSummary.getFinishReason();
  }

  public int toolCallCount() {
    return agentSummary.getToolCallCount();
  }

  public List<ToolExecution> toolExecutions() {
    if (!toolExecutionsLoaded) {
      toolExecutions = Objects.requireNonNullElse(toolExecutionsSource.get(), List.of());
      toolExecutionsLoaded = true;
    }
    return toolExecutions;
  }

  public List<ToolSummary> toolSummaries() {
    return Objects.requireNonNullElse(agentSummary.getToolSummaries(), List.of());
  }

  public List<GuardrailSummary> guardrailSummaries() {
    return Objects.requireNonNullElse(agentSummary.getGuardrailSummaries(), List.of());
  }
}
