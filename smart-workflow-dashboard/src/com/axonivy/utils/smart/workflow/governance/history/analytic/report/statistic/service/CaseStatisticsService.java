package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.CaseStatistics;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.TokenUsageSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;

public class CaseStatisticsService {

  private interface Cost {
    double PER_MILLION        = 1_000_000.0;
    double INPUT_PER_MILLION  = 10.0;
    double OUTPUT_PER_MILLION = 30.0;
  }

  private record CaseAggregates(
      int agentCount, int totalTokens, long totalDurationMs,
      int totalToolCalls, int totalMessages, long anomalyAgents,
      int totalAnomalies, int totalErrors, int totalNullResults,
      long lengthCount, long distinctModels, String processName) {}

  private record ToolTotals(int errors, int nullResults) {}

  private record TokenCosts(
      TokenUsageSummary tokenUsage, double outputInputRatio,
      double estInputCost, double estOutputCost) {}

  private CaseStatisticsService() {}

  public static CaseStatistics compute(String caseId, String caseName, List<AgentSummary> summaries) {
    CaseStatistics stats = new CaseStatistics();
    stats.caseId    = caseId;
    stats.caseName  = caseName;
    stats.summaries = summaries;

    CaseAggregates agg     = computeAggregates(summaries);
    stats.agentCount       = agg.agentCount();
    stats.totalTokens      = agg.totalTokens();
    stats.totalDurationMs  = agg.totalDurationMs();
    stats.totalToolCalls   = agg.totalToolCalls();
    stats.totalMessages    = agg.totalMessages();
    stats.anomalyAgents    = agg.anomalyAgents();
    stats.totalAnomalies   = agg.totalAnomalies();
    stats.totalErrors      = agg.totalErrors();
    stats.totalNullResults = agg.totalNullResults();
    stats.processName      = agg.processName();
    stats.lengthCount      = agg.lengthCount();
    stats.distinctModels   = agg.distinctModels();

    stats.avgTokensPerMsg     = stats.totalMessages > 0 ? (double) stats.totalTokens / stats.totalMessages : 0;
    stats.avgDurationPerAgent = stats.agentCount   > 0 ? (double) stats.totalDurationMs / stats.agentCount : 0;

    TokenCosts costs       = computeTokenCosts(summaries);
    stats.tokenUsage       = costs.tokenUsage();
    stats.outputInputRatio = costs.outputInputRatio();
    stats.estInputCost     = costs.estInputCost();
    stats.estOutputCost    = costs.estOutputCost();

    stats.toolSuccesses          = stats.totalToolCalls - stats.totalNullResults - stats.totalErrors;
    stats.overallToolSuccessRate = sharePct(stats.toolSuccesses, stats.totalToolCalls);
    stats.toolStats              = computeToolStats(summaries);
    stats.agentStats             = computeAgentStats(summaries, stats.totalTokens, stats.totalDurationMs);

    CaseObservationsService.populateObservations(stats, stats.agentStats, summaries,
        stats.totalTokens, stats.totalDurationMs, stats.totalErrors, stats.totalToolCalls);

    return stats;
  }

  private static CaseAggregates computeAggregates(List<AgentSummary> summaries) {
    int totalTokens = 0, totalToolCalls = 0, totalMessages = 0, totalAnomalies = 0;
    int totalErrors = 0, totalNullResults = 0;
    long totalDurationMs = 0, anomalyAgents = 0, lengthCount = 0;
    String processName = null;
    Set<String> models = new HashSet<>();

    for (var agent : summaries) {
      totalTokens     += agent.getTotalTokens();
      totalDurationMs += agent.getDurationMs();
      totalToolCalls  += agent.getToolCallCount();
      totalMessages   += agent.getMessageCount();
      if (agent.hasAnomalyIssues()) {
        anomalyAgents++;
        totalAnomalies += agent.getAnomalyIssues().size();
      }
      if (agent.getToolSummaries() != null) {
        var toolTotals    = accumulateToolTotals(agent.getToolSummaries());
        totalErrors      += toolTotals.errors();
        totalNullResults += toolTotals.nullResults();
      }
      if (processName == null && Optional.ofNullable(agent).map(AgentSummary::getProcessName).orElse("").isBlank() == false) {
        processName = agent.getProcessName();
      }
      if (FinishReasons.LENGTH.matches(agent.getFinishReason())) {
        lengthCount++;
      }
      if (agent.getModel() != null) {
        models.add(agent.getModel());
      }
    }

    return new CaseAggregates(summaries.size(), totalTokens, totalDurationMs,
        totalToolCalls, totalMessages, anomalyAgents, totalAnomalies, totalErrors,
        totalNullResults, lengthCount, models.size(), processName);
  }

  private static TokenCosts computeTokenCosts(List<AgentSummary> summaries) {
    TokenUsageSummary ts = summaries.isEmpty() ? null : summaries.get(0).getTokenUsageSummary();
    double outputInputRatio = (ts != null && ts.getTotalInputTokens() > 0)
        ? (double) ts.getTotalOutputTokens() / ts.getTotalInputTokens() : 0;
    double estInputCost  = ts != null ? estimateCost(ts.getTotalInputTokens(),  Cost.INPUT_PER_MILLION)  : 0;
    double estOutputCost = ts != null ? estimateCost(ts.getTotalOutputTokens(), Cost.OUTPUT_PER_MILLION) : 0;
    return new TokenCosts(ts, outputInputRatio, estInputCost, estOutputCost);
  }

  private static List<CaseStatistics.ToolStats> computeToolStats(List<AgentSummary> summaries) {
    Map<String, List<ToolSummary>> byTool = summaries.stream()
        .flatMap(CaseStatisticsService::toolStream)
        .collect(Collectors.groupingBy(ToolSummary::getToolName));

    return byTool.entrySet().stream().map(entry -> {
      List<ToolSummary> toolList = entry.getValue();
      int calls = toolList.stream().mapToInt(ToolSummary::getCallCount).sum();
      int nulls = toolList.stream().mapToInt(ToolSummary::getNullResultCount).sum();
      int errs  = toolList.stream().mapToInt(ToolSummary::getErrorCount).sum();
      int succ  = calls - nulls - errs;
      int rate  = sharePct(succ, calls);
      String grade = gradeToolSuccessRate(rate);
      return new CaseStatistics.ToolStats(entry.getKey(), calls, succ, nulls, errs, rate, grade);
    }).toList();
  }

  private static List<CaseStatistics.AgentStats> computeAgentStats(List<AgentSummary> summaries,
      int totalTokens, long totalDurationMs) {
    List<CaseStatistics.AgentStats> result = new ArrayList<>();
    for (int i = 0; i < summaries.size(); i++) {
      AgentSummary summary = summaries.get(i);
      result.add(new CaseStatistics.AgentStats(
          i,
          summary.getDisplayName(),
          summary,
          tokensPerMessage(summary),
          throughputTokensPerSec(summary),
          sharePct(summary.getTotalTokens(), totalTokens),
          sharePct(summary.getDurationMs(), totalDurationMs),
          AgentGradingService.gradeAgent(summary, totalTokens, totalDurationMs)));
    }
    return Collections.unmodifiableList(result);
  }

  private static double tokensPerMessage(AgentSummary s) {
    return s.getMessageCount() > 0 ? (double) s.getTotalTokens() / s.getMessageCount() : 0;
  }

  private static double throughputTokensPerSec(AgentSummary s) {
    return (s.getDurationMs() > 0 && s.getTotalTokens() > 0)
        ? s.getTotalTokens() * 1000.0 / s.getDurationMs() : 0;
  }

  private static int sharePct(long value, long total) {
    return total > 0 ? (int) (value * 100 / total) : 0;
  }

  private static double estimateCost(int tokens, double ratePerMillion) {
    return tokens / Cost.PER_MILLION * ratePerMillion;
  }

  private static String gradeToolSuccessRate(int rate) {
    if (rate >= 90) {
      return "A";
    }
    if (rate >= 70) {
      return "B";
    }
    if (rate >= 50) {
      return "C";
    }
    return "D";
  }

  private static ToolTotals accumulateToolTotals(List<ToolSummary> tools) {
    return new ToolTotals(
        tools.stream().mapToInt(ToolSummary::getErrorCount).sum(),
        tools.stream().mapToInt(ToolSummary::getNullResultCount).sum());
  }

  private static Stream<ToolSummary> toolStream(AgentSummary s) {
    var tools = s.getToolSummaries();
    return tools != null ? tools.stream() : Stream.empty();
  }
}
