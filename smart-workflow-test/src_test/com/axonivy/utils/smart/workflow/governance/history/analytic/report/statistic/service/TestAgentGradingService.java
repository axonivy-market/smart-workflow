package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentGradingService {

  @Test
  void gradeAgent_noIssues_returnsGradeA() {
    var agent = agent(200, 5_000, null, null, null, null);
    // score = 100, no penalties → A
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("A");
  }

  @Test
  void gradeAgent_lengthFinishReason_penalizedByTwentyPoints() {
    var agent = agent(200, 5_000, null, null, null, "LENGTH");
    // score = 100 - 20 (length) = 80 → B
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("B");
  }

  @Test
  void gradeAgent_twoToolErrors_penalizedByTwentyPoints() {
    var tool = new ToolSummary("search", 5, 0, 2, null);
    var agent = agent(200, 5_000, List.of(tool), null, null, null);
    // score = 100 - (2 errors * 10) = 80 → B
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("B");
  }

  @Test
  void gradeAgent_oneAnomaly_penalizedByFifteenPoints() {
    var agent = agent(200, 5_000, null, null, List.of("Duration exceeded 30s"), null);
    // score = 100 - (1 anomaly * 15) = 85 → B
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("B");
  }

  @Test
  void gradeAgent_oneFatalGuardrail_penalizedByTwentyFivePoints() {
    var guardrail = new GuardrailSummary("pii-check", 0, 0, 1, 0);
    var agent = agent(200, 5_000, null, List.of(guardrail), null, null);
    // score = 100 - (1 fatal * 25) = 75 → B
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("B");
  }

  @Test
  void gradeAgent_tokenHog_penalizedByTenPoints() {
    // 600 of 1000 total tokens = 60% > 50%
    var agent = agent(600, 5_000, null, null, null, null);
    // score = 100 - 10 (token hog) = 90 → A
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("A");
  }

  @Test
  void gradeAgent_timeHog_penalizedByFivePoints() {
    // 12000 of 20000 ms = 60% > 50%
    var agent = agent(200, 12_000, null, null, null, null);
    // score = 100 - 5 (time hog) = 95 → A
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("A");
  }

  @Test
  void gradeAgent_manyPenalties_scoreClampedToZero() {
    var tool = new ToolSummary("search", 5, 0, 10, null);      // 10 errors × 10 = 100
    var guardrail = new GuardrailSummary("pii", 0, 0, 5, 0);   // 5 fatals × 25 = 125
    var agent = agent(600, 12_000, List.of(tool), List.of(guardrail),
        List.of("a1", "a2", "a3"), "LENGTH");
    // combined penalties far exceed 100 → clamped to 0 → F
    assertThat(AgentGradingService.gradeAgent(agent, 1_000, 20_000)).startsWith("F");
  }

  @Test
  void gradeAgent_zeroTotalTokensAndDuration_noHogPenalty() {
    var agent = agent(0, 0, null, null, null, null);
    // sharePct returns 0 when total is 0 → no hog penalties → score 100 → A
    assertThat(AgentGradingService.gradeAgent(agent, 0, 0)).startsWith("A");
  }

  private static AgentSummary agent(int tokens, long durationMs,
      List<ToolSummary> tools, List<GuardrailSummary> guardrails,
      List<String> anomalies, String finishReason) {
    var agent = new AgentSummary();
    agent.setTotalTokens(tokens);
    agent.setDurationMs(durationMs);
    agent.setToolSummaries(tools);
    agent.setGuardrailSummaries(guardrails);
    agent.setAnomalyIssues(anomalies);
    agent.setFinishReason(finishReason);
    return agent;
  }
}
