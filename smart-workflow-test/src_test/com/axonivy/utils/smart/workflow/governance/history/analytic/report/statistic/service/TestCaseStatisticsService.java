package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCaseStatisticsService {

  @Test
  void compute_emptySummaries_returnsZeroAggregates() {
    var stats = CaseStatisticsService.compute("case-1", "My Case", List.of());
    assertThat(stats.agentCount).isEqualTo(0);
    assertThat(stats.totalTokens).isEqualTo(0);
    assertThat(stats.totalDurationMs).isEqualTo(0L);
    assertThat(stats.totalToolCalls).isEqualTo(0);
    assertThat(stats.totalMessages).isEqualTo(0);
    assertThat(stats.agentStats).isEmpty();
  }

  @Test
  void compute_singleAgent_populatesCaseIdAndName() {
    var stats = CaseStatisticsService.compute("case-42", "Invoice Process", List.of(agent(100, 1000)));
    assertThat(stats.caseId).isEqualTo("case-42");
    assertThat(stats.caseName).isEqualTo("Invoice Process");
  }

  @Test
  void compute_multipleAgents_sumsTokensAndDuration() {
    var summaries = List.of(agent(200, 1_000), agent(300, 2_000));
    var stats = CaseStatisticsService.compute("c", "n", summaries);
    assertThat(stats.totalTokens).isEqualTo(500);
    assertThat(stats.totalDurationMs).isEqualTo(3_000L);
    assertThat(stats.agentCount).isEqualTo(2);
  }

  @Test
  void compute_agentWithAnomalies_countedInAnomalyAgents() {
    var withAnomaly    = agentWithAnomalies(List.of("Duration exceeded 30s", "Token spike"));
    var withoutAnomaly = agent(100, 500);
    var stats = CaseStatisticsService.compute("c", "n", List.of(withAnomaly, withoutAnomaly));
    assertThat(stats.anomalyAgents).isEqualTo(1);
    assertThat(stats.totalAnomalies).isEqualTo(2);
  }

  @Test
  void compute_agentsWithTools_aggregatesErrorsAndNulls() {
    var tool1 = new ToolSummary("search", 5, 1, 1, null);
    var tool2 = new ToolSummary("fetch",  3, 0, 2, null);
    var a1 = agentWithTools(List.of(tool1));
    var a2 = agentWithTools(List.of(tool2));
    var stats = CaseStatisticsService.compute("c", "n", List.of(a1, a2));
    assertThat(stats.totalErrors).isEqualTo(3);       // 1 + 2
    assertThat(stats.totalNullResults).isEqualTo(1);  // 1 + 0
  }

  @Test
  void compute_processName_takesFirstNonBlankFromAgents() {
    var noProcess   = agent(100, 500);
    var withProcess = agentWithProcess("Approval Process");
    var stats = CaseStatisticsService.compute("c", "n", List.of(noProcess, withProcess));
    assertThat(stats.processName).isEqualTo("Approval Process");
  }

  @Test
  void compute_distinctModels_countedCorrectly() {
    var a1 = agentWithModel("gpt-4");
    var a2 = agentWithModel("gpt-4");
    var a3 = agentWithModel("claude-3");
    var stats = CaseStatisticsService.compute("c", "n", List.of(a1, a2, a3));
    assertThat(stats.distinctModels).isEqualTo(2);
  }

  @Test
  void compute_lengthFinishReason_counted() {
    var normal = agentWithFinishReason("STOP");
    var length = agentWithFinishReason("LENGTH");
    var stats = CaseStatisticsService.compute("c", "n", List.of(normal, length, length));
    assertThat(stats.lengthCount).isEqualTo(2);
  }

  @Test
  void compute_avgTokensPerMsg_computed() {
    var a = agent(500, 1_000);
    a.setMessageCount(10);
    var stats = CaseStatisticsService.compute("c", "n", List.of(a));
    assertThat(stats.avgTokensPerMsg).isEqualTo(50.0, offset(0.01));
  }

  @Test
  void compute_toolStats_groupedByToolName() {
    var tool = new ToolSummary("search", 4, 1, 0, null);
    var a1 = agentWithTools(List.of(tool));
    var a2 = agentWithTools(List.of(tool));
    var stats = CaseStatisticsService.compute("c", "n", List.of(a1, a2));
    assertThat(stats.toolStats).hasSize(1);
    assertThat(stats.toolStats.get(0).toolName()).isEqualTo("search");
    assertThat(stats.toolStats.get(0).calls()).isEqualTo(8);  // 4 + 4
  }

  @Test
  void compute_agentStats_indexedInOrder() {
    var a1 = agentWithName("Alpha");
    var a2 = agentWithName("Beta");
    var stats = CaseStatisticsService.compute("c", "n", List.of(a1, a2));
    assertThat(stats.agentStats).hasSize(2);
    assertThat(stats.agentStats.get(0).index()).isEqualTo(0);
    assertThat(stats.agentStats.get(1).index()).isEqualTo(1);
  }

  @Test
  void compute_slowestAgent_identified() {
    var fast = agent(100, 500);
    var slow = agent(100, 5_000);
    var stats = CaseStatisticsService.compute("c", "n", List.of(fast, slow));
    assertThat(stats.slowestAgent).isNotNull();
    assertThat(stats.slowestAgent.getSummary().getDurationMs()).isEqualTo(5_000L);
  }

  @Test
  void compute_highestTokenAgent_identified() {
    var low  = agent(100, 500);
    var high = agent(900, 500);
    var stats = CaseStatisticsService.compute("c", "n", List.of(low, high));
    assertThat(stats.highestTokenAgent).isNotNull();
    assertThat(stats.highestTokenAgent.getSummary().getTotalTokens()).isEqualTo(900);
  }

  @Test
  void compute_longConversationAgents_aboveThreshold() {
    var normal = agent(100, 500);
    normal.setMessageCount(10);
    var long_ = agent(100, 500);
    long_.setMessageCount(21);
    var stats = CaseStatisticsService.compute("c", "n", List.of(normal, long_));
    assertThat(stats.longConversationAgents).hasSize(1);
    assertThat(stats.longConversationAgents.get(0).getMessageCount()).isEqualTo(21);
  }

  @Test
  void compute_modelList_distinctCommaSeparated() {
    var a1 = agentWithModel("gpt-4");
    var a2 = agentWithModel("claude-3");
    var a3 = agentWithModel("gpt-4");
    var stats = CaseStatisticsService.compute("c", "n", List.of(a1, a2, a3));
    assertThat(stats.modelList).contains("gpt-4").contains("claude-3");
    // only 2 distinct models
    long commaCount = stats.modelList.chars().filter(c -> c == ',').count();
    assertThat(commaCount).isEqualTo(1);
  }

  @Test
  void compute_toolSuccessRate_computedFromTotals() {
    // 6 calls, 1 null, 1 error → 4 successes → 66%
    var tool = new ToolSummary("t", 6, 1, 1, null);
    var a = agentWithTools(List.of(tool));
    a.setToolCallCount(6);
    var stats = CaseStatisticsService.compute("c", "n", List.of(a));
    assertThat(stats.toolSuccesses).isEqualTo(4);
    assertThat(stats.overallToolSuccessRate).isEqualTo(66);
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  private static AgentSummary agent(int tokens, long durationMs) {
    var a = new AgentSummary();
    a.setAgentId("agent-id");
    a.setTotalTokens(tokens);
    a.setDurationMs(durationMs);
    a.setMessageCount(5);
    a.setToolCallCount(0);
    a.setFinishReason("STOP");
    return a;
  }

  private static AgentSummary agentWithAnomalies(List<String> anomalies) {
    var a = agent(100, 500);
    a.setAnomalyIssues(anomalies);
    return a;
  }

  private static AgentSummary agentWithTools(List<ToolSummary> tools) {
    var a = agent(100, 500);
    a.setToolSummaries(tools);
    a.setToolCallCount(tools.stream().mapToInt(ToolSummary::getCallCount).sum());
    return a;
  }

  private static AgentSummary agentWithProcess(String processName) {
    var a = agent(100, 500);
    a.setProcessName(processName);
    return a;
  }

  private static AgentSummary agentWithModel(String model) {
    var a = agent(100, 500);
    a.setModel(model);
    return a;
  }

  private static AgentSummary agentWithFinishReason(String reason) {
    var a = agent(100, 500);
    a.setFinishReason(reason);
    return a;
  }

  private static AgentSummary agentWithName(String name) {
    var a = agent(100, 500);
    a.setAgentName(name);
    return a;
  }
}
