package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentStats;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.Observations;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.Observations.AgentObservation;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;

class CaseObservationsService {

  private static final int LONG_CONVERSATION = 20;

  private CaseObservationsService() {}

  public static Observations computeObservations(List<AgentStats> agentStats,
      List<AgentSummary> summaries, int totalTokens, long totalDurationMs) {
    AgentStats slowestAgent = agentStats.stream()
        .max(Comparator.comparingLong(agentStat -> agentStat.getSummary().getDurationMs())).orElse(null);
    AgentStats highestTokenAgent = agentStats.stream()
        .max(Comparator.comparingInt(agentStat -> agentStat.getSummary().getTotalTokens())).orElse(null);

    return new Observations(
        toObservation(slowestAgent),
        slowestAgent != null ? sharePct(slowestAgent.getSummary().getDurationMs(), totalDurationMs) : 0,
        toObservation(highestTokenAgent),
        highestTokenAgent != null ? sharePct(highestTokenAgent.getSummary().getTotalTokens(), totalTokens) : 0,
        buildModels(summaries),
        countUnexpectedFinishes(summaries),
        findLongConversationAgents(agentStats));
  }

  private static AgentObservation toObservation(AgentStats agent) {
    return agent == null ? null : new AgentObservation(
        agent.getIndex(), agent.getDisplayName(), agent.getSummary().getMessageCount(),
        agent.getSummary().getTotalTokens(), agent.getSummary().getDurationMs());
  }

  private static List<String> buildModels(List<AgentSummary> summaries) {
    return summaries.stream()
        .map(AgentSummary::getModel).filter(Objects::nonNull).distinct()
        .toList();
  }

  private static long countUnexpectedFinishes(List<AgentSummary> summaries) {
    return summaries.stream()
        .filter(summary -> FinishReasons.isUnexpected(summary.getFinishReason())).count();
  }

  private static List<AgentObservation> findLongConversationAgents(List<AgentStats> agentStats) {
    return agentStats.stream()
        .filter(agent -> agent.getSummary().getMessageCount() > LONG_CONVERSATION)
        .map(CaseObservationsService::toObservation)
        .toList();
  }

  private static int sharePct(long value, long total) {
    return total > 0 ? (int) (value * 100 / total) : 0;
  }
}
