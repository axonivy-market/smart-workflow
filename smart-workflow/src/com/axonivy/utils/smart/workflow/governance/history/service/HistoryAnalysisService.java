package com.axonivy.utils.smart.workflow.governance.history.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.AnomalyReport;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder.ResponseMetadata;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;

public class HistoryAnalysisService {

  private static final IvyRepoHistoryStorage STORAGE = new IvyRepoHistoryStorage();

  public static List<AgentSummary> analyze(String caseId) {
    List<AgentConversationEntry> entries = STORAGE.findByCaseUuid(caseId);
    List<ToolSummary> allToolSummaries = buildToolSummaries(entries);
    List<GuardrailSummary> allGuardrailSummaries = buildGuardrailSummaries(entries);
    TokenUsageSummary tokenUsageSummary = buildTokenUsageSummary(entries);
    List<AgentSummary> summaries = entries.stream()
        .map(e -> summarize(e, allToolSummaries, allGuardrailSummaries, tokenUsageSummary))
        .toList();
    Ivy.log().info("Summaries for caseId {0}: {1}", caseId, summaries);
    return summaries;
  }

  public static int countAgents(List<AgentSummary> summaries) {
    return summaries != null ? summaries.size() : 0;
  }

  public static int totalCaseTokens(List<AgentSummary> summaries) {
    if (summaries == null) return 0;
    return summaries.stream().mapToInt(AgentSummary::getTotalTokens).sum();
  }

  public static long totalCaseDurationMs(List<AgentSummary> summaries) {
    if (summaries == null) return 0L;
    return summaries.stream().mapToLong(AgentSummary::getDurationMs).sum();
  }

  public static int countAnomalyAgents(List<AgentSummary> summaries) {
    if (summaries == null) return 0;
    return (int) summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues())
        .count();
  }

  public static String toJson(List<AgentSummary> summaries) {
    try {
      return JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(summaries);
    } catch (Exception e) {
      return "[]";
    }
  }

  public static String generateReport(String caseId, List<AgentSummary> summaries) {
    if (summaries == null || summaries.isEmpty()) {
      return "No agent data available for case " + caseId + ".";
    }

    StringBuilder sb = new StringBuilder();
    int agentCount = summaries.size();
    int totalTokens = summaries.stream().mapToInt(AgentSummary::getTotalTokens).sum();
    long totalDurationMs = summaries.stream().mapToLong(AgentSummary::getDurationMs).sum();
    int totalToolCalls = summaries.stream().mapToInt(AgentSummary::getToolCallCount).sum();
    int totalMessages = summaries.stream().mapToInt(AgentSummary::getMessageCount).sum();
    long anomalyAgents = summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()).count();
    int totalAnomalies = summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().getIssues() != null)
        .mapToInt(s -> s.getAnomalyReport().getIssues().size()).sum();

    // Header
    sb.append("═══════════════════════════════════════════════════\n");
    sb.append("  AI AGENT CASE ANALYSIS REPORT\n");
    sb.append("═══════════════════════════════════════════════════\n");
    sb.append("Case ID: ").append(caseId).append("\n\n");

    // Executive Summary
    sb.append("── EXECUTIVE SUMMARY ──────────────────────────────\n\n");
    sb.append("This case involved ").append(agentCount).append(" AI agent")
        .append(agentCount != 1 ? "s" : "").append(" that collectively processed ")
        .append(totalMessages).append(" messages, consumed ").append(formatNumber(totalTokens))
        .append(" tokens, and executed ").append(totalToolCalls).append(" tool call")
        .append(totalToolCalls != 1 ? "s" : "").append(".\n\n");
    sb.append("Total processing time: ").append(formatDuration(totalDurationMs)).append("\n");

    if (anomalyAgents == 0) {
      sb.append("Health status: ALL CLEAN — no anomalies detected across any agent.\n\n");
    } else {
      sb.append("Health status: ").append(anomalyAgents).append(" agent")
          .append(anomalyAgents != 1 ? "s" : "").append(" flagged with a total of ")
          .append(totalAnomalies).append(" anomal").append(totalAnomalies != 1 ? "ies" : "y")
          .append(".\n\n");
    }

    // Token Usage Overview
    TokenUsageSummary tokenSummary = summaries.get(0).getTokenUsageSummary();
    if (tokenSummary != null) {
      sb.append("── TOKEN USAGE ────────────────────────────────────\n\n");
      sb.append("  Input tokens:  ").append(formatNumber(tokenSummary.getTotalInputTokens())).append("\n");
      sb.append("  Output tokens: ").append(formatNumber(tokenSummary.getTotalOutputTokens())).append("\n");
      sb.append("  Total tokens:  ").append(formatNumber(tokenSummary.getTotalTokens())).append("\n");
      double ratio = tokenSummary.getTotalInputTokens() > 0
          ? (double) tokenSummary.getTotalOutputTokens() / tokenSummary.getTotalInputTokens() : 0;
      sb.append("  Output/Input ratio: ").append(String.format("%.2f", ratio)).append("\n");
      sb.append("  Max single conversation: ").append(formatNumber(tokenSummary.getMaxSingleConversationTokens())).append(" tokens\n");

      if (ratio > 3.0) {
        sb.append("\n  ⚠ The output/input ratio is high (>3.0), indicating verbose responses\n");
        sb.append("    or possible over-generation. Consider tightening prompt instructions.\n");
      } else if (ratio < 0.2) {
        sb.append("\n  ⚠ The output/input ratio is low (<0.2), indicating very terse responses.\n");
        sb.append("    Verify that agents are producing sufficiently detailed outputs.\n");
      }
      sb.append("\n");
    }

    // Per-Agent Breakdown
    sb.append("── AGENT BREAKDOWN ────────────────────────────────\n\n");
    for (int i = 0; i < summaries.size(); i++) {
      AgentSummary s = summaries.get(i);
      sb.append("Agent #").append(i + 1).append("  [").append(s.getAgentId()).append("]\n");
      sb.append("  Model: ").append(s.getModel() != null ? s.getModel() : "N/A");
      sb.append("  |  Finish: ").append(s.getFinishReason() != null ? s.getFinishReason() : "N/A").append("\n");
      sb.append("  Messages: ").append(s.getMessageCount());
      sb.append("  |  Tool calls: ").append(s.getToolCallCount());
      sb.append("  |  Tokens: ").append(formatNumber(s.getTotalTokens()));
      sb.append("  |  Duration: ").append(formatDuration(s.getDurationMs())).append("\n");

      // Token share
      if (totalTokens > 0) {
        int pct = s.getTotalTokens() * 100 / totalTokens;
        sb.append("  Token share of case: ").append(pct).append("%\n");
      }

      // Tools
      if (s.getToolSummaries() != null && !s.getToolSummaries().isEmpty()) {
        sb.append("  Tools used:\n");
        for (ToolSummary ts : s.getToolSummaries()) {
          int successCount = ts.getCallCount() - ts.getNullResultCount() - ts.getErrorCount();
          int successRate = ts.getCallCount() > 0 ? successCount * 100 / ts.getCallCount() : 0;
          sb.append("    • ").append(ts.getToolName())
              .append(" — ").append(ts.getCallCount()).append(" call(s), ")
              .append(successRate).append("% success");
          if (ts.getNullResultCount() > 0) {
            sb.append(", ").append(ts.getNullResultCount()).append(" null");
          }
          if (ts.getErrorCount() > 0) {
            sb.append(", ").append(ts.getErrorCount()).append(" error(s)");
          }
          sb.append("\n");
        }
      }

      // Guardrails
      if (s.getGuardrailSummaries() != null && !s.getGuardrailSummaries().isEmpty()) {
        sb.append("  Guardrails:\n");
        for (GuardrailSummary gs : s.getGuardrailSummaries()) {
          int total = gs.getPassedCount() + gs.getFailedCount() + gs.getFatalCount();
          sb.append("    • ").append(gs.getGuardrailName())
              .append(" — ").append(gs.getPassedCount()).append(" passed");
          if (gs.getFailedCount() > 0) {
            sb.append(", ").append(gs.getFailedCount()).append(" failed");
          }
          if (gs.getFatalCount() > 0) {
            sb.append(", ").append(gs.getFatalCount()).append(" FATAL");
          }
          sb.append(" (avg ").append(String.format("%.1f", gs.getAvgDurationMs())).append("ms)");
          if (total > 1) {
            sb.append(" [ran ").append(total).append("x]");
          }
          sb.append("\n");
        }
      }

      // Anomalies
      if (s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()) {
        sb.append("  ⚠ Anomalies:\n");
        for (String issue : s.getAnomalyReport().getIssues()) {
          sb.append("    ✗ ").append(issue).append("\n");
        }
      } else {
        sb.append("  ✓ No anomalies\n");
      }
      sb.append("\n");
    }

    // Observations and recommendations
    sb.append("── OBSERVATIONS ───────────────────────────────────\n\n");

    // Duration analysis
    AgentSummary slowest = summaries.stream()
        .max((a, b) -> Long.compare(a.getDurationMs(), b.getDurationMs())).orElse(null);
    if (slowest != null && totalDurationMs > 0) {
      int slowestPct = (int) (slowest.getDurationMs() * 100 / totalDurationMs);
      sb.append("• Slowest agent: #").append(summaries.indexOf(slowest) + 1)
          .append(" (").append(formatDuration(slowest.getDurationMs()))
          .append(", ").append(slowestPct).append("% of total case time)\n");
    }

    // Token hog
    AgentSummary tokenHog = summaries.stream()
        .max((a, b) -> Integer.compare(a.getTotalTokens(), b.getTotalTokens())).orElse(null);
    if (tokenHog != null && totalTokens > 0) {
      int hogPct = tokenHog.getTotalTokens() * 100 / totalTokens;
      sb.append("• Highest token consumer: #").append(summaries.indexOf(tokenHog) + 1)
          .append(" (").append(formatNumber(tokenHog.getTotalTokens()))
          .append(" tokens, ").append(hogPct).append("% of case total)\n");
    }

    // Model diversity
    long distinctModels = summaries.stream()
        .map(AgentSummary::getModel).filter(m -> m != null).distinct().count();
    sb.append("• Models used: ").append(distinctModels).append(" distinct model")
        .append(distinctModels != 1 ? "s" : "").append("\n");

    // Finish reason check
    long lengthCount = summaries.stream()
        .filter(s -> "LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    if (lengthCount > 0) {
      sb.append("• ⚠ ").append(lengthCount).append(" agent(s) finished with LENGTH — output was truncated\n");
    }

    long nonStopCount = summaries.stream()
        .filter(s -> s.getFinishReason() != null && !"STOP".equalsIgnoreCase(s.getFinishReason())
            && !"LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    if (nonStopCount > 0) {
      sb.append("• ⚠ ").append(nonStopCount)
          .append(" agent(s) finished with a non-standard reason — investigate potential errors\n");
    }

    // All clean?
    if (anomalyAgents == 0 && lengthCount == 0) {
      sb.append("\n✓ Overall: case completed cleanly with no anomalies or truncations.\n");
    }

    sb.append("\n═══════════════════════════════════════════════════\n");
    sb.append("  END OF REPORT\n");
    sb.append("═══════════════════════════════════════════════════\n");

    return sb.toString();
  }

  private static String formatNumber(int n) {
    return String.format("%,d", n);
  }

  private static String formatDuration(long ms) {
    if (ms < 1000) return ms + "ms";
    double secs = ms / 1000.0;
    if (secs < 60) return String.format("%.1fs", secs);
    long mins = ms / 60000;
    long remainSecs = (ms % 60000) / 1000;
    return mins + "m " + remainSecs + "s";
  }

  private static AgentSummary summarize(AgentConversationEntry entry, List<ToolSummary> allToolSummaries,
      List<GuardrailSummary> allGuardrailSummaries, TokenUsageSummary tokenUsageSummary) {
    List<ToolExecution> tools = entry.getToolExecutions();
    int toolCallCount = tools != null ? tools.size() : 0;
    Set<String> entryToolNames = tools != null
        ? tools.stream().map(ToolExecution::toolName).collect(Collectors.toSet())
        : Collections.emptySet();
    List<ToolSummary> toolSummaries = allToolSummaries.stream()
        .filter(ts -> entryToolNames.contains(ts.getToolName()))
        .collect(Collectors.toList());

    List<ResponseMetadata> metadataList = parseMetadata(entry.getTokenUsageJson());
    int totalTokens = metadataList.stream().mapToInt(m -> m.totalTokens() != null ? m.totalTokens() : 0).sum();
    long durationMs = metadataList.stream().mapToLong(m -> m.durationMs() != null ? m.durationMs() : 0).sum();
    String finishReason = metadataList.isEmpty() ? null : metadataList.getLast().finishReason();
    String model = metadataList.isEmpty() ? null : metadataList.getLast().modelName();

    List<GuardrailExecution> guardrails = entry.getGuardrailExecutions();
    Set<String> entryGuardrailNames = guardrails != null
        ? guardrails.stream().map(GuardrailExecution::guardrailName).collect(Collectors.toSet())
        : Collections.emptySet();
    List<GuardrailSummary> guardrailSummaries = allGuardrailSummaries.stream()
        .filter(gs -> entryGuardrailNames.contains(gs.getGuardrailName()))
        .collect(Collectors.toList());

    return new AgentSummary(entry.getAgentId(), countMessages(entry.getMessagesJson()),
        toolCallCount, totalTokens, durationMs, finishReason, model, toolSummaries, guardrailSummaries, tokenUsageSummary,
        buildAnomalyReport(tools, toolSummaries, guardrailSummaries, totalTokens, durationMs, finishReason));
  }

  private static AnomalyReport buildAnomalyReport(List<ToolExecution> tools,
      List<ToolSummary> toolSummaries, List<GuardrailSummary> guardrailSummaries,
      int totalTokens, long durationMs, String finishReason) {
    List<String> issues = new ArrayList<>();

    if (durationMs > 30_000) {
      issues.add("Duration exceeded 30s (" + durationMs + "ms)");
    }
    if (totalTokens > 10_000) {
      issues.add("Tokens exceeded 10,000 per conversation (" + totalTokens + ")");
    }
    int toolCallCount = tools != null ? tools.size() : 0;
    if (toolCallCount > 10) {
      issues.add("More than 10 tool calls (" + toolCallCount + ") — possible loop");
    }
    if (guardrailSummaries != null) {
      guardrailSummaries.stream()
          .filter(gs -> gs.getFatalCount() > 0)
          .forEach(gs -> issues.add("FATAL guardrail result on '" + gs.getGuardrailName() + "' (" + gs.getFatalCount() + " time(s))"));
    }
    if ("LENGTH".equalsIgnoreCase(finishReason)) {
      issues.add("Finish reason is LENGTH — output was truncated");
    }
    if (toolSummaries != null) {
      toolSummaries.stream()
          .filter(ts -> ts.getCallCount() > 0 && (double) ts.getNullResultCount() / ts.getCallCount() > 0.5)
          .forEach(ts -> issues.add("Tool '" + ts.getToolName() + "' returned null/empty in "
              + ts.getNullResultCount() + "/" + ts.getCallCount() + " calls (>50%)"));
    }
    if (tools != null) {
      tools.stream()
          .filter(t -> t.arguments() != null)
          .collect(Collectors.groupingBy(t -> t.toolName() + "::" + t.arguments(), Collectors.counting()))
          .entrySet().stream()
          .filter(e -> e.getValue() > 5)
          .forEach(e -> {
            String[] parts = e.getKey().split("::", 2);
            issues.add("Tool '" + parts[0] + "' called " + e.getValue()
                + " times with identical arguments — possible stuck loop");
          });
    }
    return new AnomalyReport(issues);
  }

  private static List<GuardrailSummary> buildGuardrailSummaries(List<AgentConversationEntry> entries) {
    List<GuardrailExecution> allGuardrails = entries.stream()
        .flatMap(e -> e.getGuardrailExecutions() != null ? e.getGuardrailExecutions().stream() : java.util.stream.Stream.empty())
        .collect(Collectors.toList());
    return allGuardrails.stream()
        .collect(Collectors.groupingBy(GuardrailExecution::guardrailName))
        .entrySet().stream()
        .map(eg -> {
          List<GuardrailExecution> group = eg.getValue();
          int passedCount = (int) group.stream().filter(g -> "PASSED".equalsIgnoreCase(g.result()) || "SUCCESS".equalsIgnoreCase(g.result())).count();
          int failedCount = (int) group.stream().filter(g -> "FAILED".equalsIgnoreCase(g.result())).count();
          int fatalCount  = (int) group.stream().filter(g -> "FATAL".equalsIgnoreCase(g.result())).count();
          double avgDurationMs = group.stream()
              .filter(g -> g.durationMs() != null)
              .mapToLong(GuardrailExecution::durationMs)
              .average().orElse(0);
          return new GuardrailSummary(eg.getKey(), passedCount, failedCount, fatalCount, avgDurationMs);
        })
        .collect(Collectors.toList());
  }

  private static TokenUsageSummary buildTokenUsageSummary(List<AgentConversationEntry> entries) {
    List<ResponseMetadata> allMetadata = entries.stream()
        .flatMap(e -> parseMetadata(e.getTokenUsageJson()).stream())
        .collect(Collectors.toList());
    if (allMetadata.isEmpty()) {
      return new TokenUsageSummary(0, 0, 0, 0, 0, 0, 0);
    }
    int totalInput  = allMetadata.stream().mapToInt(m -> m.inputTokens()  != null ? m.inputTokens()  : 0).sum();
    int totalOutput = allMetadata.stream().mapToInt(m -> m.outputTokens() != null ? m.outputTokens() : 0).sum();
    int totalAll    = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).sum();
    double avgInput  = allMetadata.stream().mapToInt(m -> m.inputTokens()  != null ? m.inputTokens()  : 0).average().orElse(0);
    double avgOutput = allMetadata.stream().mapToInt(m -> m.outputTokens() != null ? m.outputTokens() : 0).average().orElse(0);
    double avgTotal  = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).average().orElse(0);
    int maxSingle    = allMetadata.stream().mapToInt(m -> m.totalTokens()  != null ? m.totalTokens()  : 0).max().orElse(0);
    return new TokenUsageSummary(totalInput, totalOutput, totalAll, avgInput, avgOutput, avgTotal, maxSingle);
  }

  private static List<ToolSummary> buildToolSummaries(List<AgentConversationEntry> entries) {
    List<ToolExecution> allTools = entries.stream()
        .flatMap(e -> e.getToolExecutions() != null ? e.getToolExecutions().stream() : java.util.stream.Stream.empty())
        .collect(Collectors.toList());
    return allTools.stream()
        .collect(Collectors.groupingBy(ToolExecution::toolName))
        .entrySet().stream()
        .map(eg -> {
          List<ToolExecution> group = eg.getValue();
          int callCount = group.size();
          int nullResultCount = (int) group.stream().filter(t -> StringUtils.isBlank(t.resultText())).count();
          int errorCount = (int) group.stream()
              .filter(t -> t.resultText() != null && t.resultText().toLowerCase().contains("error")).count();
          String sampleArguments = group.stream()
              .map(ToolExecution::arguments)
              .filter(a -> !StringUtils.isBlank(a))
              .findFirst().orElse(null);
          return new ToolSummary(eg.getKey(), callCount, nullResultCount, errorCount, sampleArguments);
        })
        .collect(Collectors.toList());
  }

  private static int countMessages(String messagesJson) {
    if (messagesJson == null || messagesJson.isBlank()) {
      return 0;
    }
    try {
      var node = JsonUtils.getObjectMapper().readTree(messagesJson);
      if (node.isArray()) {
        return node.size();
      }
      var arr = node.get("messages");
      return arr != null && arr.isArray() ? arr.size() : 0;
    } catch (Exception e) {
      return 0;
    }
  }

  private static List<ResponseMetadata> parseMetadata(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return JsonUtils.getObjectMapper().readValue(json, new TypeReference<List<ResponseMetadata>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
