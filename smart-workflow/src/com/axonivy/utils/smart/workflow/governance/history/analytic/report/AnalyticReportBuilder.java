package com.axonivy.utils.smart.workflow.governance.history.analytic.report;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.entity.summary.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.summary.ToolSummary;

class AnalyticReportBuilder {

  private static final String HEADER_TOP = """
      ═══════════════════════════════════════════════════════════
        AI AGENT CASE ANALYSIS REPORT
      ═══════════════════════════════════════════════════════════
        Case    : %s
        UUID    : %s
      """;

  private static final String HEADER_BOTTOM = """
        Agents  : %d
        Duration: %s
        Tokens  : %s
      ═══════════════════════════════════════════════════════════

      """;

  private static final String EXECUTIVE_SUMMARY = """
      ── EXECUTIVE SUMMARY ──────────────────────────────────────

      This case involved %d AI agent%s that collectively processed \
      %d messages, consumed %s tokens, and executed %d tool call%s.

        Total processing time : %s
        Avg tokens per message: %.0f
        Avg duration per agent: %s
      """;

  private static final String TOKEN_USAGE_TABLE = """
      ── TOKEN USAGE ────────────────────────────────────────────

        ┌─────────────────────┬──────────────┬──────────────┐
        │                     │    Total     │   Average    │
        ├─────────────────────┼──────────────┼──────────────┤
        │ Input tokens        │ %12s │ %12s │
        │ Output tokens       │ %12s │ %12s │
        │ Total tokens        │ %12s │ %12s │
        └─────────────────────┴──────────────┴──────────────┘

        Output/Input ratio     : %.2f
        Max single conversation: %s tokens

        Estimated cost (GPT-4 class pricing):
          Input  (~$10/1M tokens) : $%.4f
          Output (~$30/1M tokens) : $%.4f
          Combined estimate       : $%.4f
          Note: Actual cost depends on the specific model and provider.
      """;

  private static final String TOKEN_RATIO_HIGH = """

          ⚠ ATTENTION: Output/Input ratio is high (%.2f > 3.0).
          This indicates verbose or over-generated responses.
          Suggestion: Tighten system prompt instructions. Add constraints like
          'respond concisely' or 'limit response to key facts only'.
        """;

  private static final String TOKEN_RATIO_LOW = """

          ⚠ ATTENTION: Output/Input ratio is low (%.2f < 0.2).
          Agents may be producing insufficiently detailed outputs.
          Suggestion: Review prompts for overly restrictive instructions.
        """;

  private static final String TOOL_EFFECTIVENESS_HEADER = """
      ── TOOL EFFECTIVENESS ─────────────────────────────────────

        Total tool calls : %d
        Successful       : %d (%d%%)
        Null/empty       : %d
        Errors           : %d

      """;

  private static final String TOOL_PER_TOOL = """
              %s
                Calls: %d  |  Success: %d (%d%%)  |  Grade: %s
          """;

  private static final String AGENT_METRICS = """
      │  Model   : %s
      │  Finish  : %s
      │  Messages: %d  |  Tool calls: %d
      │  Tokens  : %s  |  Duration: %s
      """;

  private static final String GUARDRAIL_FATAL_NOTE = """
      │      → FATAL guardrail violations require immediate investigation.
      │        Review the agent's prompt and input data for policy violations.
      """;

  private static final String OBSERVATION_SLOWEST_NOTE = """
            → This agent dominates processing time. Consider if its task can be
              decomposed into smaller sub-tasks or optimized with fewer messages.
          """;

  private static final String OBSERVATION_TOKEN_HOG_NOTE = """
            → This agent uses >60%% of all tokens. Review if the prompt is too broad
              or if the conversation can be condensed with summarization techniques.
          """;

  private static final String OBSERVATION_MULTI_MODEL = """
          → Multiple models are in use. Ensure each agent uses the most cost-effective
            model for its complexity level. Simple tasks may work with lighter models.
        """;

  private static final String OBSERVATION_LENGTH = """
        • ⚠ %d agent(s) finished with LENGTH — output was truncated.
          → Increase max_tokens or reduce prompt complexity so the model can
            complete its response without hitting the token limit.
        """;

  private static final String OBSERVATION_NON_STOP = """
        • ⚠ %d agent(s) finished with a non-standard reason.
          → Investigate for API errors, content filtering, or unexpected terminations.
        """;

  private static final String OBSERVATION_HIGH_TOOL_ERROR = """
            → High tool error rate. Investigate tool implementations, input validation,
              and whether the agent is calling tools with incorrect arguments.
          """;

  private static final String REC_ANOMALIES = """
      %d. RESOLVE ANOMALIES: %d agent(s) have flagged issues. Review the anomaly details above
         and address root causes before deploying to production.

      """;

  private static final String REC_TOKEN_USAGE = """
      %d. OPTIMIZE TOKEN USAGE: Total tokens (%s) are high.
         Consider: shorter system prompts, conversation summarization,
         or switching verbose agents to more concise models.

      """;

  private static final String REC_LATENCY = """
      %d. IMPROVE LATENCY: Total case duration (%s) exceeds 1 minute.
         Consider: parallel agent execution, caching frequent tool results,
         or pre-computing common lookups.

      """;

  private static final String REC_TOOL_ERRORS = """
      %d. FIX TOOL ERRORS: %d tool error(s) detected across the case.
         Investigate error responses and add input validation or retry logic.

      """;

  private static final String REC_NULL_RESULTS = """
      %d. REDUCE NULL RESULTS: %d/%d tool calls returned null/empty.
         Ensure tools return meaningful fallback messages instead of null,
         and verify the agent is calling tools with valid parameters.

      """;

  private static final String REC_TRUNCATION = """
      %d. PREVENT TRUNCATION: %d agent(s) hit the token limit.
         Increase max_tokens configuration or simplify expected outputs.

      """;

  private static final String REC_MODEL_MIX = """
      %d. CONSIDER MODEL MIX: All %d agents use the same model.
         Simpler agents (e.g., routing, classification) may perform equally
         well with a lighter, faster, cheaper model.

      """;

  private static final String REC_NO_ISSUES = """
      No critical recommendations. The case executed within healthy parameters.
      Continue monitoring for patterns across multiple cases.

      """;

  private static final String FOOTER = """

      ═══════════════════════════════════════════════════════════
        END OF REPORT
      ═══════════════════════════════════════════════════════════
      """;

  private final String caseId;
  private final String caseName;
  private final List<AgentSummary> summaries;

  private final int agentCount;
  private final int totalTokens;
  private final long totalDurationMs;
  private final int totalToolCalls;
  private final int totalMessages;
  private final long anomalyAgents;
  private final int totalAnomalies;
  private final int totalErrors;
  private final int totalNullResults;
  private final String processName;
  private final long lengthCount;
  private final long distinctModels;

  AnalyticReportBuilder(String caseId, String caseName, List<AgentSummary> summaries) {
    this.caseId = caseId;
    this.caseName = caseName;
    this.summaries = summaries;
    this.agentCount = summaries.size();
    this.totalTokens = summaries.stream().mapToInt(AgentSummary::getTotalTokens).sum();
    this.totalDurationMs = summaries.stream().mapToLong(AgentSummary::getDurationMs).sum();
    this.totalToolCalls = summaries.stream().mapToInt(AgentSummary::getToolCallCount).sum();
    this.totalMessages = summaries.stream().mapToInt(AgentSummary::getMessageCount).sum();
    this.anomalyAgents = summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()).count();
    this.totalAnomalies = summaries.stream()
        .filter(s -> s.getAnomalyReport() != null && s.getAnomalyReport().getIssues() != null)
        .mapToInt(s -> s.getAnomalyReport().getIssues().size()).sum();
    this.totalErrors = summaries.stream()
        .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
        .mapToInt(ToolSummary::getErrorCount).sum();
    this.totalNullResults = summaries.stream()
        .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
        .mapToInt(ToolSummary::getNullResultCount).sum();
    this.processName = summaries.stream()
        .map(AgentSummary::getProcessName).filter(p -> p != null && !p.isBlank())
        .findFirst().orElse(null);
    this.lengthCount = summaries.stream()
        .filter(s -> "LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    this.distinctModels = summaries.stream()
        .map(AgentSummary::getModel).filter(m -> m != null).distinct().count();
  }

  String render() {
    var sb = new StringBuilder();
    sb.append(renderHeader());
    sb.append(renderExecutiveSummary());
    sb.append(renderTokenUsage());
    sb.append(renderToolEffectiveness());
    sb.append(renderAgentBreakdown());
    sb.append(renderObservations());
    sb.append(renderRecommendations());
    sb.append(renderFooter());
    return sb.toString();
  }

  private String renderHeader() {
    var sb = new StringBuilder();
    sb.append(HEADER_TOP.formatted(caseName != null ? caseName : caseId, caseId));
    if (processName != null) {
      sb.append("  Process : ").append(processName).append("\n");
    }
    sb.append(HEADER_BOTTOM.formatted(agentCount, formatDuration(totalDurationMs), formatNumber(totalTokens)));
    return sb.toString();
  }

  private String renderExecutiveSummary() {
    var sb = new StringBuilder();
    String agentPlural = agentCount != 1 ? "s" : "";
    String toolPlural = totalToolCalls != 1 ? "s" : "";
    double avgTokensPerMsg = totalMessages > 0 ? (double) totalTokens / totalMessages : 0;
    double avgDurationPerAgent = agentCount > 0 ? (double) totalDurationMs / agentCount : 0;

    sb.append(EXECUTIVE_SUMMARY.formatted(agentCount, agentPlural, totalMessages, formatNumber(totalTokens),
        totalToolCalls, toolPlural, formatDuration(totalDurationMs),
        avgTokensPerMsg, formatDuration((long) avgDurationPerAgent)));

    if (totalToolCalls > 0) {
      double avgTokensPerTool = (double) totalTokens / totalToolCalls;
      sb.append("  Avg tokens per tool call: %.0f\n".formatted(avgTokensPerTool));
    }
    sb.append("\n");

    if (anomalyAgents == 0) {
      sb.append("  Health: ✓ ALL CLEAN — no anomalies detected across any agent.\n\n");
    } else {
      String anomalyPlural = anomalyAgents != 1 ? "s" : "";
      String anomalyWord = totalAnomalies != 1 ? "ies" : "y";
      sb.append("  Health: ⚠ %d agent%s flagged with %d anomal%s total.\n\n"
          .formatted(anomalyAgents, anomalyPlural, totalAnomalies, anomalyWord));
    }
    return sb.toString();
  }

  private String renderTokenUsage() {
    TokenUsageSummary ts = summaries.get(0).getTokenUsageSummary();
    if (ts == null) {
      return "";
    }

    double ratio = ts.getTotalInputTokens() > 0
        ? (double) ts.getTotalOutputTokens() / ts.getTotalInputTokens() : 0;
    double estInputCost = ts.getTotalInputTokens() / 1_000_000.0 * 10.0;
    double estOutputCost = ts.getTotalOutputTokens() / 1_000_000.0 * 30.0;

    var sb = new StringBuilder();
    sb.append(TOKEN_USAGE_TABLE.formatted(
        formatNumber(ts.getTotalInputTokens()), "%.0f".formatted(ts.getAvgInputTokens()),
        formatNumber(ts.getTotalOutputTokens()), "%.0f".formatted(ts.getAvgOutputTokens()),
        formatNumber(ts.getTotalTokens()), "%.0f".formatted(ts.getAvgTotalTokens()),
        ratio, formatNumber(ts.getMaxSingleConversationTokens()),
        estInputCost, estOutputCost, estInputCost + estOutputCost));

    if (ratio > 3.0) {
      sb.append(TOKEN_RATIO_HIGH.formatted(ratio));
    } else if (ratio < 0.2) {
      sb.append(TOKEN_RATIO_LOW.formatted(ratio));
    }
    sb.append("\n");
    return sb.toString();
  }

  private String renderToolEffectiveness() {
    if (totalToolCalls <= 0) {
      return "";
    }

    int toolSuccesses = totalToolCalls - totalNullResults - totalErrors;
    int overallSuccessRate = toolSuccesses * 100 / totalToolCalls;

    var sb = new StringBuilder();
    sb.append(TOOL_EFFECTIVENESS_HEADER.formatted(
        totalToolCalls, toolSuccesses, overallSuccessRate, totalNullResults, totalErrors));

    List<ToolSummary> allTools = summaries.stream()
        .flatMap(s -> s.getToolSummaries() != null ? s.getToolSummaries().stream() : java.util.stream.Stream.<ToolSummary>empty())
        .collect(Collectors.toList());
    Map<String, List<ToolSummary>> byTool = allTools.stream().collect(Collectors.groupingBy(ToolSummary::getToolName));

    if (!byTool.isEmpty()) {
      sb.append("  Per-tool breakdown:\n");
      byTool.forEach((name, toolList) -> {
        int calls = toolList.stream().mapToInt(ToolSummary::getCallCount).sum();
        int nulls = toolList.stream().mapToInt(ToolSummary::getNullResultCount).sum();
        int errs = toolList.stream().mapToInt(ToolSummary::getErrorCount).sum();
        int succ = calls - nulls - errs;
        int rate = calls > 0 ? succ * 100 / calls : 0;
        String grade = rate >= 90 ? "A" : rate >= 70 ? "B" : rate >= 50 ? "C" : "D";
        sb.append(TOOL_PER_TOOL.formatted(name, calls, succ, rate, grade));
        if (errs > 0) {
          sb.append("      → %d error(s) detected. Review tool implementation or input data.\n".formatted(errs));
        }
        if (nulls > 0 && calls > 0 && nulls * 100 / calls > 30) {
          sb.append("      → High null-result rate (%d%%). Check if the tool handles edge cases properly.\n"
              .formatted(nulls * 100 / calls));
        }
      });
      sb.append("\n");
    }
    return sb.toString();
  }

  private String renderAgentBreakdown() {
    var sb = new StringBuilder();
    sb.append("── AGENT BREAKDOWN ────────────────────────────────────────\n\n");
    for (int i = 0; i < summaries.size(); i++) {
      sb.append(renderSingleAgent(i, summaries.get(i)));
    }
    return sb.toString();
  }

  private String renderSingleAgent(int index, AgentSummary s) {
    String displayName = (s.getAgentName() != null && !s.getAgentName().isBlank())
        ? s.getAgentName() : s.getAgentId();

    var sb = new StringBuilder();
    sb.append("┌─ Agent #%d  %s ─────────────────────────────\n".formatted(index + 1, displayName));
    if (s.getAgentName() != null && !s.getAgentName().isBlank()) {
      sb.append("│  ID     : %s\n".formatted(s.getAgentId()));
    }
    sb.append(AGENT_METRICS.formatted(
        s.getModel() != null ? s.getModel() : "N/A",
        s.getFinishReason() != null ? s.getFinishReason() : "N/A",
        s.getMessageCount(), s.getToolCallCount(),
        formatNumber(s.getTotalTokens()), formatDuration(s.getDurationMs())));

    if (s.getMessageCount() > 0) {
      sb.append("│  Tokens/message: %.0f\n".formatted((double) s.getTotalTokens() / s.getMessageCount()));
    }
    if (s.getDurationMs() > 0 && s.getTotalTokens() > 0) {
      double tokensPerSec = s.getTotalTokens() * 1000.0 / s.getDurationMs();
      sb.append("│  Throughput: %.1f tokens/sec\n".formatted(tokensPerSec));
    }
    if (totalTokens > 0) {
      int pct = s.getTotalTokens() * 100 / totalTokens;
      sb.append("│  Token share: %d%% of case total  [%s]\n".formatted(pct, progressBar(pct)));
    }
    if (totalDurationMs > 0) {
      int dPct = (int) (s.getDurationMs() * 100 / totalDurationMs);
      sb.append("│  Time share  : %d%% of case total\n".formatted(dPct));
    }

    if (s.getToolSummaries() != null && !s.getToolSummaries().isEmpty()) {
      sb.append("│\n│  Tools:\n");
      for (ToolSummary ts : s.getToolSummaries()) {
        sb.append(renderToolLine(ts));
      }
    }
    if (s.getGuardrailSummaries() != null && !s.getGuardrailSummaries().isEmpty()) {
      sb.append("│\n│  Guardrails:\n");
      for (GuardrailSummary gs : s.getGuardrailSummaries()) {
        sb.append(renderGuardrailLine(gs));
      }
    }
    if (s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()) {
      sb.append("│\n│  ⚠ Anomalies (%d):\n".formatted(s.getAnomalyReport().getIssues().size()));
      for (String issue : s.getAnomalyReport().getIssues()) {
        sb.append("│    ✗ %s\n".formatted(issue));
      }
    } else {
      sb.append("│\n│  ✓ No anomalies detected\n");
    }

    String grade = gradeAgent(s, totalTokens, totalDurationMs);
    sb.append("│\n│  Overall grade: %s\n".formatted(grade));
    sb.append("└──────────────────────────────────────────────────────\n\n");
    return sb.toString();
  }

  private String renderToolLine(ToolSummary ts) {
    int successCount = ts.getCallCount() - ts.getNullResultCount() - ts.getErrorCount();
    int successRate = ts.getCallCount() > 0 ? successCount * 100 / ts.getCallCount() : 0;
    String indicator = successRate >= 90 ? "✓" : successRate >= 50 ? "~" : "✗";
    var sb = new StringBuilder();
    sb.append("│    %s %s — %d call(s), %d%% success".formatted(indicator, ts.getToolName(), ts.getCallCount(), successRate));
    if (ts.getNullResultCount() > 0) {
      sb.append(", %d null".formatted(ts.getNullResultCount()));
    }
    if (ts.getErrorCount() > 0) {
      sb.append(", %d error(s)".formatted(ts.getErrorCount()));
    }
    sb.append("\n");
    return sb.toString();
  }

  private String renderGuardrailLine(GuardrailSummary gs) {
    int total = gs.getPassedCount() + gs.getFailedCount() + gs.getFatalCount();
    String indicator = gs.getFatalCount() > 0 ? "✗" : gs.getFailedCount() > 0 ? "~" : "✓";
    var sb = new StringBuilder();
    sb.append("│    %s %s — %d passed".formatted(indicator, gs.getGuardrailName(), gs.getPassedCount()));
    if (gs.getFailedCount() > 0) {
      sb.append(", %d failed".formatted(gs.getFailedCount()));
    }
    if (gs.getFatalCount() > 0) {
      sb.append(", %d FATAL".formatted(gs.getFatalCount()));
    }
    sb.append(" (avg %.1fms)".formatted(gs.getAvgDurationMs()));
    if (total > 1) {
      sb.append(" [ran %dx]".formatted(total));
    }
    sb.append("\n");
    if (gs.getFatalCount() > 0) {
      sb.append(GUARDRAIL_FATAL_NOTE);
    }
    if (gs.getAvgDurationMs() > 500) {
      sb.append("│      → Guardrail avg duration >500ms. Consider optimizing the check.\n");
    }
    return sb.toString();
  }

  private String renderObservations() {
    var sb = new StringBuilder();
    sb.append("── OBSERVATIONS ───────────────────────────────────────────\n\n");

    AgentSummary slowest = summaries.stream()
        .max((a, b) -> Long.compare(a.getDurationMs(), b.getDurationMs())).orElse(null);
    if (slowest != null && totalDurationMs > 0) {
      int slowestPct = (int) (slowest.getDurationMs() * 100 / totalDurationMs);
      sb.append("• Slowest agent: #%d [%s] (%s, %d%% of total case time)\n"
          .formatted(summaries.indexOf(slowest) + 1, displayName(slowest),
              formatDuration(slowest.getDurationMs()), slowestPct));
      if (slowestPct > 70) {
        sb.append(OBSERVATION_SLOWEST_NOTE);
      }
    }

    AgentSummary tokenHog = summaries.stream()
        .max((a, b) -> Integer.compare(a.getTotalTokens(), b.getTotalTokens())).orElse(null);
    if (tokenHog != null && totalTokens > 0) {
      int hogPct = tokenHog.getTotalTokens() * 100 / totalTokens;
      sb.append("• Highest token consumer: #%d [%s] (%s tokens, %d%%)\n"
          .formatted(summaries.indexOf(tokenHog) + 1, displayName(tokenHog),
              formatNumber(tokenHog.getTotalTokens()), hogPct));
      if (hogPct > 60) {
        sb.append(OBSERVATION_TOKEN_HOG_NOTE);
      }
    }

    sb.append("• Models used: %d distinct model%s".formatted(distinctModels, distinctModels != 1 ? "s" : ""));
    if (distinctModels > 1) {
      String modelList = summaries.stream()
          .map(AgentSummary::getModel).filter(m -> m != null).distinct()
          .collect(Collectors.joining(", "));
      sb.append(" — ").append(modelList);
    }
    sb.append("\n");
    if (distinctModels > 1) {
      sb.append(OBSERVATION_MULTI_MODEL);
    }

    if (lengthCount > 0) {
      sb.append(OBSERVATION_LENGTH.formatted(lengthCount));
    }

    long nonStopCount = summaries.stream()
        .filter(s -> s.getFinishReason() != null && !"STOP".equalsIgnoreCase(s.getFinishReason())
            && !"LENGTH".equalsIgnoreCase(s.getFinishReason())).count();
    if (nonStopCount > 0) {
      sb.append(OBSERVATION_NON_STOP.formatted(nonStopCount));
    }

    if (totalToolCalls > 0 && totalErrors > 0) {
      int errorRate = totalErrors * 100 / totalToolCalls;
      sb.append("• Tool error rate: %d%% (%d/%d)\n".formatted(errorRate, totalErrors, totalToolCalls));
      if (errorRate > 20) {
        sb.append(OBSERVATION_HIGH_TOOL_ERROR);
      }
    }

    summaries.stream()
        .filter(s -> s.getMessageCount() > 20)
        .forEach(s -> {
          sb.append("• Agent [%s] has %d messages — long conversation.\n".formatted(displayName(s), s.getMessageCount()));
          sb.append("  → Consider adding conversation summarization to reduce context window usage.\n");
        });

    sb.append("\n");
    return sb.toString();
  }

  private String renderRecommendations() {
    var sb = new StringBuilder();
    sb.append("── RECOMMENDATIONS ────────────────────────────────────────\n\n");
    int recNum = 1;

    if (anomalyAgents > 0) {
      sb.append(REC_ANOMALIES.formatted(recNum++, anomalyAgents));
    }
    if (totalTokens > 50_000) {
      sb.append(REC_TOKEN_USAGE.formatted(recNum++, formatNumber(totalTokens)));
    }
    if (totalDurationMs > 60_000) {
      sb.append(REC_LATENCY.formatted(recNum++, formatDuration(totalDurationMs)));
    }
    if (totalErrors > 0) {
      sb.append(REC_TOOL_ERRORS.formatted(recNum++, totalErrors));
    }
    if (totalNullResults > 0 && totalToolCalls > 0 && totalNullResults * 100 / totalToolCalls > 20) {
      sb.append(REC_NULL_RESULTS.formatted(recNum++, totalNullResults, totalToolCalls));
    }
    if (lengthCount > 0) {
      sb.append(REC_TRUNCATION.formatted(recNum++, lengthCount));
    }
    if (distinctModels == 1 && agentCount > 2 && totalTokens > 10_000) {
      sb.append(REC_MODEL_MIX.formatted(recNum++, agentCount));
    }
    if (recNum == 1) {
      sb.append(REC_NO_ISSUES);
    }
    if (anomalyAgents == 0 && lengthCount == 0 && totalErrors == 0) {
      sb.append("✓ Overall: Case completed cleanly with no anomalies, truncations, or tool errors.\n");
    }
    return sb.toString();
  }

  private String renderFooter() {
    return FOOTER;
  }

  private static String displayName(AgentSummary s) {
    return (s.getAgentName() != null && !s.getAgentName().isBlank()) ? s.getAgentName() : s.getAgentId();
  }

  private static String progressBar(int pct) {
    int barLen = pct / 5;
    var sb = new StringBuilder();
    for (int b = 0; b < 20; b++) {
      sb.append(b < barLen ? "█" : "░");
    }
    return sb.toString();
  }

  private static String gradeAgent(AgentSummary s, int totalTokens, long totalDurationMs) {
    int score = 100;
    if (s.getAnomalyReport() != null && s.getAnomalyReport().hasIssues()) {
      score -= s.getAnomalyReport().getIssues().size() * 15;
    }
    if ("LENGTH".equalsIgnoreCase(s.getFinishReason())) {
      score -= 20;
    }
    if (s.getToolSummaries() != null) {
      int errs = s.getToolSummaries().stream().mapToInt(ToolSummary::getErrorCount).sum();
      score -= errs * 10;
    }
    if (s.getGuardrailSummaries() != null) {
      int fatals = s.getGuardrailSummaries().stream().mapToInt(GuardrailSummary::getFatalCount).sum();
      score -= fatals * 25;
    }
    if (totalTokens > 0 && s.getTotalTokens() * 100 / totalTokens > 50) {
      score -= 10;
    }
    if (totalDurationMs > 0 && s.getDurationMs() * 100 / totalDurationMs > 50) {
      score -= 5;
    }
    score = Math.max(0, Math.min(100, score));
    if (score >= 90) return "A (%d/100) — Excellent".formatted(score);
    if (score >= 75) return "B (%d/100) — Good".formatted(score);
    if (score >= 60) return "C (%d/100) — Fair, needs attention".formatted(score);
    if (score >= 40) return "D (%d/100) — Poor, investigate issues".formatted(score);
    return "F (%d/100) — Critical, requires immediate action".formatted(score);
  }

  static String formatNumber(int n) {
    return String.format("%,d", n);
  }

  static String formatDuration(long ms) {
    if (ms < 1000) return ms + "ms";
    double secs = ms / 1000.0;
    if (secs < 60) return String.format("%.1fs", secs);
    long mins = ms / 60000;
    long remainSecs = (ms % 60000) / 1000;
    return mins + "m " + remainSecs + "s";
  }
}
