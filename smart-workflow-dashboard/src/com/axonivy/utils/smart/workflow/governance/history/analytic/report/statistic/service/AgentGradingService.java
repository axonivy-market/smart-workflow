package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.AgentGrade;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums.FinishReasons;

class AgentGradingService {

  private interface Penalty {
    int ANOMALY         = 15;
    int LENGTH          = 20;
    int TOOL_ERROR      = 10;
    int FATAL_GUARDRAIL = 25;
    int TOKEN_HOG       = 10;
    int TIME_HOG        = 5;
  }

  private static final int DOMINANT_SHARE_PCT = 50;

  private AgentGradingService() {}

  public static String gradeAgent(AgentSummary agent, int totalTokens, long totalDurationMs) {
    int score = 100
        - anomalyPenalty(agent)
        - lengthPenalty(agent)
        - toolErrorPenalty(agent)
        - guardrailPenalty(agent)
        - tokenHogPenalty(agent, totalTokens)
        - timeHogPenalty(agent, totalDurationMs);
    score = Math.max(0, Math.min(100, score));
    return AgentGrade.from(score).format(score);
  }

  private static int anomalyPenalty(AgentSummary agent) {
    return agent.hasAnomalyIssues() ? agent.getAnomalyIssues().size() * Penalty.ANOMALY : 0;
  }

  private static int lengthPenalty(AgentSummary agent) {
    return FinishReasons.LENGTH.matches(agent.getFinishReason()) ? Penalty.LENGTH : 0;
  }

  private static int toolErrorPenalty(AgentSummary agent) {
    if (agent.getToolSummaries() == null) {
      return 0;
    }
    return agent.getToolSummaries().stream().mapToInt(ToolSummary::getErrorCount).sum() * Penalty.TOOL_ERROR;
  }

  private static int guardrailPenalty(AgentSummary agent) {
    if (agent.getGuardrailSummaries() == null) {
      return 0;
    }
    return agent.getGuardrailSummaries().stream().mapToInt(GuardrailSummary::getFatalCount).sum() * Penalty.FATAL_GUARDRAIL;
  }

  private static int tokenHogPenalty(AgentSummary agent, int totalTokens) {
    return sharePct(agent.getTotalTokens(), totalTokens) > DOMINANT_SHARE_PCT ? Penalty.TOKEN_HOG : 0;
  }

  private static int timeHogPenalty(AgentSummary agent, long totalDurationMs) {
    return sharePct(agent.getDurationMs(), totalDurationMs) > DOMINANT_SHARE_PCT ? Penalty.TIME_HOG : 0;
  }

  private static int sharePct(long value, long total) {
    return total > 0 ? (int) (value * 100 / total) : 0;
  }
}
