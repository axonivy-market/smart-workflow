package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

class AnomalyDetectionService {

  private interface AnomalyMessages {
    String DURATION        = "Duration exceeded 30s (%dms)";
    String TOKENS          = "Tokens exceeded 10,000 per conversation (%d)";
    String TOOL_CALLS      = "More than 10 tool calls (%d): possible loop";
    String FATAL_GUARDRAIL = "FATAL guardrail result on '%s' (%d time(s))";
    String LENGTH          = "Finish reason is LENGTH: output was truncated";
    String NULL_RESULT     = "Tool '%s' returned null/empty in %d/%d calls (>50%%)";
    String STUCK_LOOP      = "Tool '%s' called %d times with identical arguments: possible stuck loop";
  }

  private interface AnomalyThresholds {
    long   DURATION_MS      = 30_000L;
    int    TOKENS           = 10_000;
    int    TOOL_CALLS       = 10;
    int    STUCK_LOOP_CALLS = 5;
    double NULL_RESULT      = 0.5;
  }

  private record ToolCallKey(String toolName, String arguments) {}

  private AnomalyDetectionService() {}

  public static List<String> detect(List<ToolExecution> tools,
      List<ToolSummary> toolSummaries, List<GuardrailSummary> guardrailSummaries,
      int totalTokens, long durationMs, String finishReason) {
    List<ToolExecution> safeTools = Objects.requireNonNullElse(tools, List.of());
    List<ToolSummary> safeSummaries = Objects.requireNonNullElse(toolSummaries, List.of());
    List<GuardrailSummary> safeGuardrails = Objects.requireNonNullElse(guardrailSummaries, List.of());

    List<String> issues = new ArrayList<>();
    detectExcessiveDuration(durationMs, issues);
    detectExcessiveTokenUsage(totalTokens, issues);
    detectExcessiveToolCalls(safeTools, issues);
    detectFatalGuardrailResults(safeGuardrails, issues);
    detectTruncatedOutput(finishReason, issues);
    detectHighNullResultRate(safeSummaries, issues);
    detectStuckLoops(safeTools, issues);
    return issues;
  }

  private static void detectExcessiveDuration(long durationMs, List<String> issues) {
    if (durationMs > AnomalyThresholds.DURATION_MS) {
      issues.add(AnomalyMessages.DURATION.formatted(durationMs));
    }
  }

  private static void detectExcessiveTokenUsage(int totalTokens, List<String> issues) {
    if (totalTokens > AnomalyThresholds.TOKENS) {
      issues.add(AnomalyMessages.TOKENS.formatted(totalTokens));
    }
  }

  private static void detectExcessiveToolCalls(List<ToolExecution> tools, List<String> issues) {
    if (tools.size() > AnomalyThresholds.TOOL_CALLS) {
      issues.add(AnomalyMessages.TOOL_CALLS.formatted(tools.size()));
    }
  }

  private static void detectFatalGuardrailResults(List<GuardrailSummary> guardrailSummaries, List<String> issues) {
    guardrailSummaries.stream()
        .filter(guardrail -> guardrail.getFatalCount() > 0)
        .forEach(guardrail -> issues.add(AnomalyMessages.FATAL_GUARDRAIL.formatted(guardrail.getGuardrailName(), guardrail.getFatalCount())));
  }

  private static void detectTruncatedOutput(String finishReason, List<String> issues) {
    if (FinishReasons.LENGTH.matches(finishReason)) {
      issues.add(AnomalyMessages.LENGTH);
    }
  }

  private static void detectHighNullResultRate(List<ToolSummary> toolSummaries, List<String> issues) {
    toolSummaries.stream()
        .filter(toolSummary -> toolSummary.getCallCount() > 0
            && (double) toolSummary.getNullResultCount() / toolSummary.getCallCount() > AnomalyThresholds.NULL_RESULT)
        .forEach(toolSummary -> issues.add(AnomalyMessages.NULL_RESULT.formatted(
            toolSummary.getToolName(), toolSummary.getNullResultCount(), toolSummary.getCallCount())));
  }

  private static void detectStuckLoops(List<ToolExecution> tools, List<String> issues) {
    tools.stream()
        .filter(tool -> tool.arguments() != null)
        .collect(Collectors.groupingBy(
            tool -> new ToolCallKey(tool.toolName(), tool.arguments()),
            Collectors.counting()))
        .entrySet().stream()
        .filter(entry -> entry.getValue() > AnomalyThresholds.STUCK_LOOP_CALLS)
        .forEach(entry -> issues.add(AnomalyMessages.STUCK_LOOP.formatted(entry.getKey().toolName(), entry.getValue())));
  }
}
