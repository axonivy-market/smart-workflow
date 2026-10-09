package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.CaseStatistics;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;

class CaseObservationsService {

  private static final int LONG_CONVERSATION = 20;

  private CaseObservationsService() {}

  public static void populateObservations(CaseStatistics stats, List<CaseStatistics.AgentStats> agentStats,
      List<AgentSummary> summaries, int totalTokens, long totalDurationMs,
      int totalErrors, int totalToolCalls) {
    CaseStatistics.AgentStats slowestAgent = agentStats.stream()
        .max(Comparator.comparingLong(agentStat -> agentStat.getSummary().getDurationMs())).orElse(null);
    CaseStatistics.AgentStats highestTokenAgent = agentStats.stream()
        .max(Comparator.comparingInt(agentStat -> agentStat.getSummary().getTotalTokens())).orElse(null);

    stats.slowestAgent = slowestAgent;
    stats.slowestAgentPct = slowestAgent != null ? sharePct(slowestAgent.getSummary().getDurationMs(), totalDurationMs) : 0;
    stats.highestTokenAgent = highestTokenAgent;
    stats.highestTokenPct = highestTokenAgent != null ? sharePct(highestTokenAgent.getSummary().getTotalTokens(), totalTokens) : 0;
    stats.modelList = buildModelList(summaries);
    stats.unexpectedFinishCount = countUnexpectedFinishes(summaries);
    stats.toolErrorRate = sharePct(totalErrors, totalToolCalls);
    stats.longConversationAgents = findLongConversationAgents(summaries);
  }

  private static String buildModelList(List<AgentSummary> summaries) {
    return summaries.stream()
        .map(AgentSummary::getModel).filter(Objects::nonNull).distinct()
        .collect(Collectors.joining(", "));
  }

  private static long countUnexpectedFinishes(List<AgentSummary> summaries) {
    return summaries.stream()
        .filter(summary -> FinishReasons.isUnexpected(summary.getFinishReason())).count();
  }

  private static List<AgentSummary> findLongConversationAgents(List<AgentSummary> summaries) {
    return summaries.stream()
        .filter(summary -> summary.getMessageCount() > LONG_CONVERSATION)
        .toList();
  }

  private static int sharePct(long value, long total) {
    return total > 0 ? (int) (value * 100 / total) : 0;
  }
}
