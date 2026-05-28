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
    int totalErrors = summaries.stream()
        .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
        .mapToInt(ToolSummary::getErrorCount).sum();
    int totalNullResults = summaries.stream()
        .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
        .mapToInt(ToolSummary::getNullResultCount).sum();

    // Derive process name from first summary that has one
    String processName = summaries.stream()
        .map(AgentSummary::getProcessName).filter(p -> p != null && !p.isBlank())
        .findFirst().orElse(null);

    // Header
    sb.append("═══════════════════════════════════════════════════════════\n");
    sb.append("  AI AGENT CASE ANALYSIS REPORT\n");
    sb.append("═══════════════════════════════════════════════════════════\n");
    sb.append("  Case ID : ").append(caseId).append("\n");
    if (processName != null) {
      sb.append("  Process : ").append(processName).append("\n");
    }
    sb.append("  Agents  : ").append(agentCount).append("\n");
    sb.append("  Duration: ").append(formatDuration(totalDurationMs)).append("\n");
    sb.append("  Tokens  : ").append(formatNumber(totalTokens)).append("\n");
    sb.append("═══════════════════════════════════════════════════════════\n\n");

    // Executive Summary
    sb.append("── EXECUTIVE SUMMARY ──────────────────────────────────────\n\n");
    sb.append("This case involved ").append(agentCount).append(" AI agent")
        .append(agentCount != 1 ? "s" : "").append(" that collectively processed ")
        .append(totalMessages).append(" messages, consumed ").append(formatNumber(totalTokens))
        .append(" tokens, and executed ").append(totalToolCalls).append(" tool call")
        .append(totalToolCalls != 1 ? "s" : "").append(".\n\n");
    sb.append("  Total processing time : ").append(formatDuration(totalDurationMs)).append("\n");
    double avgTokensPerMsg = totalMessages > 0 ? (double) totalTokens / totalMessages : 0;
    sb.append("  Avg tokens per message: ").append(String.format("%.0f", avgTokensPerMsg)).append("\n");
    double avgDurationPerAgent = agentCount > 0 ? (double) totalDurationMs / agentCount : 0;
    sb.append("  Avg duration per agent: ").append(formatDuration((long) avgDurationPerAgent)).append("\n");
    if (totalToolCalls > 0) {
      double avgTokensPerTool = (double) totalTokens / totalToolCalls;
      sb.append("  Avg tokens per tool call: ").append(String.format("%.0f", avgTokensPerTool)).append("\n");
    }
    sb.append("\n");

    // Health status
    if (anomalyAgents == 0) {
      sb.append("  Health: ✓ ALL CLEAN — no anomalies detected across any agent.\n\n");
    } else {
      sb.append("  Health: ⚠ ").append(anomalyAgents).append(" agent")
          .append(anomalyAgents != 1 ? "s" : "").append(" flagged with ")
          .append(totalAnomalies).append(" anomal").append(totalAnomalies != 1 ? "ies" : "y")
          .append(" total.\n\n");
    }

    // Token Usage Overview
    TokenUsageSummary tokenSummary = summaries.get(0).getTokenUsageSummary();
    if (tokenSummary != null) {
      sb.append("── TOKEN USAGE ────────────────────────────────────────────\n\n");
      sb.append("  ┌─────────────────────┬──────────────┬──────────────┐\n");
      sb.append("  │                     │    Total     │   Average    │\n");
      sb.append("  ├─────────────────────┼──────────────┼──────────────┤\n");
      sb.append(String.format("  │ Input tokens        │ %12s │ %12s │%n",
          formatNumber(tokenSummary.getTotalInputTokens()),
          String.format("%.0f", tokenSummary.getAvgInputTokens())));
      sb.append(String.format("  │ Output tokens       │ %12s │ %12s │%n",
          formatNumber(tokenSummary.getTotalOutputTokens()),
          String.format("%.0f", tokenSummary.getAvgOutputTokens())));
      sb.append(String.format("  │ Total tokens        │ %12s │ %12s │%n",
          formatNumber(tokenSummary.getTotalTokens()),
          String.format("%.0f", tokenSummary.getAvgTotalTokens())));
      sb.append("  └─────────────────────┴──────────────┴──────────────┘\n\n");

      double ratio = tokenSummary.getTotalInputTokens() > 0
          ? (double) tokenSummary.getTotalOutputTokens() / tokenSummary.getTotalInputTokens() : 0;
      sb.append("  Output/Input ratio     : ").append(String.format("%.2f", ratio)).append("\n");
      sb.append("  Max single conversation: ").append(formatNumber(tokenSummary.getMaxSingleConversationTokens())).append(" tokens\n");

      // Cost estimation (based on typical GPT-4 class pricing)
      double estInputCost = tokenSummary.getTotalInputTokens() / 1_000_000.0 * 10.0;
      double estOutputCost = tokenSummary.getTotalOutputTokens() / 1_000_000.0 * 30.0;
      sb.append("\n  Estimated cost (GPT-4 class pricing):\n");
      sb.append("    Input  (~$10/1M tokens) : $").append(String.format("%.4f", estInputCost)).append("\n");
      sb.append("    Output (~$30/1M tokens) : $").append(String.format("%.4f", estOutputCost)).append("\n");
      sb.append("    Combined estimate       : $").append(String.format("%.4f", estInputCost + estOutputCost)).append("\n");
      sb.append("    Note: Actual cost depends on the specific model and provider.\n");

      if (ratio > 3.0) {
        sb.append("\n  ⚠ ATTENTION: Output/Input ratio is high (").append(String.format("%.2f", ratio)).append(" > 3.0).\n");
        sb.append("    This indicates verbose or over-generated responses.\n");
        sb.append("    Suggestion: Tighten system prompt instructions. Add constraints like\n");
        sb.append("    'respond concisely' or 'limit response to key facts only'.\n");
      } else if (ratio < 0.2) {
        sb.append("\n  ⚠ ATTENTION: Output/Input ratio is low (").append(String.format("%.2f", ratio)).append(" < 0.2).\n");
        sb.append("    Agents may be producing insufficiently detailed outputs.\n");
        sb.append("    Suggestion: Review prompts for overly restrictive instructions.\n");
      }
      sb.append("\n");
    }

    // Tool Effectiveness Summary
    if (totalToolCalls > 0) {
      sb.append("── TOOL EFFECTIVENESS ─────────────────────────────────────\n\n");
      int toolSuccesses = totalToolCalls - totalNullResults - totalErrors;
      int overallSuccessRate = totalToolCalls > 0 ? toolSuccesses * 100 / totalToolCalls : 0;
      sb.append("  Total tool calls : ").append(totalToolCalls).append("\n");
      sb.append("  Successful       : ").append(toolSuccesses).append(" (").append(overallSuccessRate).append("%)\n");
      sb.append("  Null/empty       : ").append(totalNullResults).append("\n");
      sb.append("  Errors           : ").append(totalErrors).append("\n\n");

      // Aggregate tool table
      List<ToolSummary> allTools = summaries.stream()
          .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
          .collect(Collectors.toList());
      java.util.Map<String, List<ToolSummary>> byTool = allTools.stream().collect(Collectors.groupingBy(ToolSummary::getToolName));
      if (!byTool.isEmpty()) {
        sb.append("  Per-tool breakdown:\n");
        byTool.forEach((name, toolList) -> {
          int calls = toolList.stream().mapToInt(ToolSummary::getCallCount).sum();
          int nulls = toolList.stream().mapToInt(ToolSummary::getNullResultCount).sum();
          int errs = toolList.stream().mapToInt(ToolSummary::getErrorCount).sum();
          int succ = calls - nulls - errs;
          int rate = calls > 0 ? succ * 100 / calls : 0;
          String grade = rate >= 90 ? "A" : rate >= 70 ? "B" : rate >= 50 ? "C" : "D";
          sb.append("    ").append(name).append("\n");
          sb.append("      Calls: ").append(calls)
              .append("  |  Success: ").append(succ).append(" (").append(rate).append("%)")
              .append("  |  Grade: ").append(grade).append("\n");
          if (errs > 0) {
            sb.append("      → ").append(errs).append(" error(s) detected. Review tool implementation or input data.\n");
          }
          if (nulls > 0 && calls > 0 && nulls * 100 / calls > 30) {
            sb.append("      → High null-result rate (").append(nulls * 100 / calls)
                .append("%). Check if the tool handles edge cases properly.\n");
          }
        });
        sb.append("\n");
      }
    }

    // Per-Agent Breakdown
    sb.append("── AGENT BREAKDOWN ────────────────────────────────────────\n\n");
    for (int i = 0; i < summaries.size(); i++) {
      AgentSummary s = summaries.get(i);
      String displayName = (s.getAgentName() != null && !s.getAgentName().isBlank())
          ? s.getAgentName() : s.getAgentId();
      sb.append("┌─ Agent #").append(i + 1).append("  ").append(displayName);
      sb.append(" ─────────────────────────────\n");
      if (s.getAgentName() != null && !s.getAgentName().isBlank()) {
        sb.append("│  ID     : ").append(s.getAgentId()).append("\n");
      }
      sb.append("│  Model   : ").append(s.getModel() != null ? s.getModel() : "N/A").append("\n");
      sb.append("│  Finish  : ").append(s.getFinishReason() != null ? s.getFinishReason() : "N/A").append("\n");
      sb.append("│  Messages: ").append(s.getMessageCount());
      sb.append("  |  Tool calls: ").append(s.getToolCallCount()).append("\n");
      sb.append("│  Tokens  : ").append(formatNumber(s.getTotalTokens()));
      sb.append("  |  Duration: ").append(formatDuration(s.getDurationMs())).append("\n");

      // Efficiency metrics
      if (s.getMessageCount() > 0) {
        sb.append("│  Tokens/message: ").append(String.format("%.0f", (double) s.getTotalTokens() / s.getMessageCount())).append("\n");
      }
      if (s.getDurationMs() > 0 && s.getTotalTokens() > 0) {
        double tokensPerSec = s.getTotalTokens() * 1000.0 / s.getDurationMs();
        sb.append("│  Throughput: ").append(String.format("%.1f", tokensPerSec)).append(" tokens/sec\n");
      }

      // Token share
      if (totalTokens > 0) {
        int pct = s.getTotalTokens() * 100 / totalTokens;
        sb.append("│  Token share: ").append(pct).append("% of case total");
        int barLen = pct / 5;
        sb.append("  [");
        for (int b = 0; b < 20; b++) sb.append(b < barLen ? "█" : "░");
        sb.append("]\n");
      }

      // Duration share
      if (totalDurationMs > 0) {
        int dPct = (int) (s.getDurationMs() * 100 / totalDurationMs);
        sb.append("│  Time share  : ").append(dPct).append("% of case total\n");
      }

      // Tools
      if (s.getToolSummaries() != null && !s.getToolSummaries().isEmpty()) {
        sb.append("│\n│  Tools:\n");
        for (ToolSummary ts : s.getToolSummaries()) {
          int successCount = ts.getCallCount() - ts.getNullResultCount() - ts.getErrorCount();
          int successRate = ts.getCallCount() > 0 ? successCount * 100 / ts.getCallCount() : 0;
          String indicator = successRate >= 90 ? "✓" : successRate >= 50 ? "~" : "✗";
          sb.append("│    ").append(indicator).append(" ").append(ts.getToolName())
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
        sb.append("│\n│  Guardrails:\n");
        for (GuardrailSummary gs : s.getGuardrailSummaries()) {
          int total = gs.getPassedCount() + gs.getFailedCount() + gs.getFatalCount();
          String indicator = gs.getFatalCount() > 0 ? "✗" : gs.getFailedCount() > 0 ? "~" : "✓";
          sb.append("│    ").append(indicator).append(" ").append(gs.getGuardrailName())
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
          if (gs.getFatalCount() > 0) {
            sb.append("│      → FATAL guardrail violations require immediate investigation.\n");
            sb.append("│        Review the agent's prompt and input data for policy violations.\n");
          }
          if (gs.getAvgDurationMs() > 500) {
            sb.append("│      → Guardrail avg duration >500ms. Consider optimizing the check.\n");
          }
        }
      }

      // Anomalies
      if (s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()) {
        sb.append("│\n│  ⚠ Anomalies (").append(s.getAnomalyReport().getIssues().size()).append("):\n");
        for (String issue : s.getAnomalyReport().getIssues()) {
          sb.append("│    ✗ ").append(issue).append("\n");
        }
      } else {
        sb.append("│\n│  ✓ No anomalies detected\n");
      }

      // Per-agent grade
      String grade = gradeAgent(s, totalTokens, totalDurationMs);
      sb.append("│\n│  Overall grade: ").append(grade).append("\n");
      sb.append("└──────────────────────────────────────────────────────\n\n");
    }

    // Observations
    sb.append("── OBSERVATIONS ───────────────────────────────────────────\n\n");

    // Duration analysis
    AgentSummary slowest = summaries.stream()
        .max((a, b) -> Long.compare(a.getDurationMs(), b.getDurationMs())).orElse(null);
    if (slowest != null && totalDurationMs > 0) {
      int slowestPct = (int) (slowest.getDurationMs() * 100 / totalDurationMs);
      String slowName = (slowest.getAgentName() != null && !slowest.getAgentName().isBlank())
          ? slowest.getAgentName() : slowest.getAgentId();
      sb.append("• Slowest agent: #").append(summaries.indexOf(slowest) + 1)
          .append(" [").append(slowName).append("]")
          .append(" (").append(formatDuration(slowest.getDurationMs()))
          .append(", ").append(slowestPct).append("% of total case time)\n");
      if (slowestPct > 70) {
        sb.append("  → This agent dominates processing time. Consider if its task can be\n");
        sb.append("    decomposed into smaller sub-tasks or optimized with fewer messages.\n");
      }
    }

    // Token hog
    AgentSummary tokenHog = summaries.stream()
        .max((a, b) -> Integer.compare(a.getTotalTokens(), b.getTotalTokens())).orElse(null);
    if (tokenHog != null && totalTokens > 0) {
      int hogPct = tokenHog.getTotalTokens() * 100 / totalTokens;
      String hogName = (tokenHog.getAgentName() != null && !tokenHog.getAgentName().isBlank())
          ? tokenHog.getAgentName() : tokenHog.getAgentId();
      sb.append("• Highest token consumer: #").append(summaries.indexOf(tokenHog) + 1)
          .append(" [").append(hogName).append("]")
          .append(" (").append(formatNumber(tokenHog.getTotalTokens()))
          .append(" tokens, ").append(hogPct).append("%)\n");
      if (hogPct > 60) {
        sb.append("  → This agent uses >60% of all tokens. Review if the prompt is too broad\n");
        sb.append("    or if the conversation can be condensed with summarization techniques.\n");
      }
    }

    // Model diversity
    long distinctModels = summaries.stream()
        .map(AgentSummary::getModel).filter(m -> m != null).distinct().count();
    sb.append("• Models used: ").append(distinctModels).append(" distinct model")
        .append(distinctModels != 1 ? "s" : "");
    if (distinctModels > 1) {
      sb.append(" — ");
      String modelList = summaries.stream()
          .map(AgentSummary::getModel).filter(m -> m != null).distinct()
          .collect(Collectors.joining(", "));
      sb.append(modelList);
    }
    sb.append("\n");
    if (distinctModels > 1) {
      sb.append("  → Multiple models are in use. Ensure each agent uses the most cost-effective\n");
      sb.append("    model for its complexity level. Simple tasks may work with lighter models.\n");
    }

    // Finish reason check
    long lengthCount = summaries.stream()
        .filter(s -> "LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    if (lengthCount > 0) {
      sb.append("• ⚠ ").append(lengthCount).append(" agent(s) finished with LENGTH — output was truncated.\n");
      sb.append("  → Increase max_tokens or reduce prompt complexity so the model can\n");
      sb.append("    complete its response without hitting the token limit.\n");
    }

    long nonStopCount = summaries.stream()
        .filter(s -> s.getFinishReason() != null && !"STOP".equalsIgnoreCase(s.getFinishReason())
            && !"LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    if (nonStopCount > 0) {
      sb.append("• ⚠ ").append(nonStopCount)
          .append(" agent(s) finished with a non-standard reason.\n");
      sb.append("  → Investigate for API errors, content filtering, or unexpected terminations.\n");
    }

    // Tool error rate analysis
    if (totalToolCalls > 0 && totalErrors > 0) {
      int errorRate = totalErrors * 100 / totalToolCalls;
      sb.append("• Tool error rate: ").append(errorRate).append("% (")
          .append(totalErrors).append("/").append(totalToolCalls).append(")\n");
      if (errorRate > 20) {
        sb.append("  → High tool error rate. Investigate tool implementations, input validation,\n");
        sb.append("    and whether the agent is calling tools with incorrect arguments.\n");
      }
    }

    // High message count agents
    summaries.stream()
        .filter(s -> s.getMessageCount() > 20)
        .forEach(s -> {
          String name = (s.getAgentName() != null && !s.getAgentName().isBlank())
              ? s.getAgentName() : s.getAgentId();
          sb.append("• Agent [").append(name).append("] has ")
              .append(s.getMessageCount()).append(" messages — long conversation.\n");
          sb.append("  → Consider adding conversation summarization to reduce context window usage.\n");
        });

    sb.append("\n");

    // Recommendations
    sb.append("── RECOMMENDATIONS ────────────────────────────────────────\n\n");
    int recNum = 1;

    if (anomalyAgents > 0) {
      sb.append(recNum++).append(". RESOLVE ANOMALIES: ").append(anomalyAgents)
          .append(" agent(s) have flagged issues. Review the anomaly details above\n");
      sb.append("   and address root causes before deploying to production.\n\n");
    }

    if (totalTokens > 50_000) {
      sb.append(recNum++).append(". OPTIMIZE TOKEN USAGE: Total tokens (").append(formatNumber(totalTokens))
          .append(") are high.\n");
      sb.append("   Consider: shorter system prompts, conversation summarization,\n");
      sb.append("   or switching verbose agents to more concise models.\n\n");
    }

    if (totalDurationMs > 60_000) {
      sb.append(recNum++).append(". IMPROVE LATENCY: Total case duration (").append(formatDuration(totalDurationMs))
          .append(") exceeds 1 minute.\n");
      sb.append("   Consider: parallel agent execution, caching frequent tool results,\n");
      sb.append("   or pre-computing common lookups.\n\n");
    }

    if (totalErrors > 0) {
      sb.append(recNum++).append(". FIX TOOL ERRORS: ").append(totalErrors)
          .append(" tool error(s) detected across the case.\n");
      sb.append("   Investigate error responses and add input validation or retry logic.\n\n");
    }

    if (totalNullResults > 0 && totalToolCalls > 0 && totalNullResults * 100 / totalToolCalls > 20) {
      sb.append(recNum++).append(". REDUCE NULL RESULTS: ").append(totalNullResults).append("/")
          .append(totalToolCalls).append(" tool calls returned null/empty.\n");
      sb.append("   Ensure tools return meaningful fallback messages instead of null,\n");
      sb.append("   and verify the agent is calling tools with valid parameters.\n\n");
    }

    if (lengthCount > 0) {
      sb.append(recNum++).append(". PREVENT TRUNCATION: ").append(lengthCount)
          .append(" agent(s) hit the token limit.\n");
      sb.append("   Increase max_tokens configuration or simplify expected outputs.\n\n");
    }

    if (distinctModels == 1 && agentCount > 2 && totalTokens > 10_000) {
      sb.append(recNum++).append(". CONSIDER MODEL MIX: All ").append(agentCount)
          .append(" agents use the same model.\n");
      sb.append("   Simpler agents (e.g., routing, classification) may perform equally\n");
      sb.append("   well with a lighter, faster, cheaper model.\n\n");
    }

    if (recNum == 1) {
      sb.append("No critical recommendations. The case executed within healthy parameters.\n");
      sb.append("Continue monitoring for patterns across multiple cases.\n\n");
    }

    // All clean?
    if (anomalyAgents == 0 && lengthCount == 0 && totalErrors == 0) {
      sb.append("✓ Overall: Case completed cleanly with no anomalies, truncations, or tool errors.\n");
    }

    sb.append("\n═══════════════════════════════════════════════════════════\n");
    sb.append("  END OF REPORT\n");
    sb.append("═══════════════════════════════════════════════════════════\n");

    return sb.toString();
  }

  private static String gradeAgent(AgentSummary s, int totalTokens, long totalDurationMs) {
    int score = 100;
    // Penalize anomalies
    if (s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()) {
      score -= s.getAnomalyReport().getIssues().size() * 15;
    }
    // Penalize LENGTH finish
    if ("LENGTH".equalsIgnoreCase(s.getFinishReason())) {
      score -= 20;
    }
    // Penalize tool errors
    if (s.getToolSummaries() != null) {
      int errs = s.getToolSummaries().stream().mapToInt(ToolSummary::getErrorCount).sum();
      score -= errs * 10;
    }
    // Penalize fatal guardrails
    if (s.getGuardrailSummaries() != null) {
      int fatals = s.getGuardrailSummaries().stream().mapToInt(GuardrailSummary::getFatalCount).sum();
      score -= fatals * 25;
    }
    // Penalize if agent uses >50% of case tokens
    if (totalTokens > 0 && s.getTotalTokens() * 100 / totalTokens > 50) {
      score -= 10;
    }
    // Penalize slow agents (>50% of case time)
    if (totalDurationMs > 0 && s.getDurationMs() * 100 / totalDurationMs > 50) {
      score -= 5;
    }
    score = Math.max(0, Math.min(100, score));
    if (score >= 90) return "A (" + score + "/100) — Excellent";
    if (score >= 75) return "B (" + score + "/100) — Good";
    if (score >= 60) return "C (" + score + "/100) — Fair, needs attention";
    if (score >= 40) return "D (" + score + "/100) — Poor, investigate issues";
    return "F (" + score + "/100) — Critical, requires immediate action";
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

    return new AgentSummary(entry.getAgentId(), entry.getAgentName(), entry.getProcessName(),
        countMessages(entry.getMessagesJson()),
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
