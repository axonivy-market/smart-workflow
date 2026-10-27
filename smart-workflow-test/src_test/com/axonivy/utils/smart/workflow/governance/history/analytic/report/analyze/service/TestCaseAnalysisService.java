package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service.CaseStatisticsService;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestCaseAnalysisService {

  @Test
  void buildSummaries_sharedTools_keepMetricsScopedToEachEntry() {
    var firstEntry = entry("agent-1",
        new ToolExecution("shared-tool", "{}", "Error: failed", null),
        new GuardrailExecution("shared-guardrail", null, "FAILED", null, null, 10L, null));
    var secondEntry = entry("agent-2",
        new ToolExecution("shared-tool", "{}", null, null),
        new GuardrailExecution("shared-guardrail", null, "PASSED", null, null, 20L, null));

    var summaries = CaseAnalysisService.buildSummaries(List.of(firstEntry, secondEntry));

    assertThat(summaries).hasSize(2);
    assertThat(summaries.get(0).getToolSummaries()).singleElement()
        .satisfies(tool -> {
          assertThat(tool.getCallCount()).isEqualTo(1);
          assertThat(tool.getErrorCount()).isEqualTo(1);
          assertThat(tool.getNullResultCount()).isZero();
        });
    assertThat(summaries.get(1).getToolSummaries()).singleElement()
        .satisfies(tool -> {
          assertThat(tool.getCallCount()).isEqualTo(1);
          assertThat(tool.getErrorCount()).isZero();
          assertThat(tool.getNullResultCount()).isEqualTo(1);
        });
    assertThat(summaries.get(0).getGuardrailSummaries()).singleElement()
        .satisfies(guardrail -> {
          assertThat(guardrail.getFailedCount()).isEqualTo(1);
          assertThat(guardrail.getPassedCount()).isZero();
        });
    assertThat(summaries.get(1).getGuardrailSummaries()).singleElement()
        .satisfies(guardrail -> {
          assertThat(guardrail.getPassedCount()).isEqualTo(1);
          assertThat(guardrail.getFailedCount()).isZero();
        });

    var caseStats = CaseStatisticsService.compute("case-1", "Case", summaries);
    assertThat(caseStats.getToolMetrics().getTotalCalls()).isEqualTo(2);
    assertThat(caseStats.getToolMetrics().getTotalErrors()).isEqualTo(1);
    assertThat(caseStats.getToolMetrics().getTotalNullResults()).isEqualTo(1);
  }

  private static AgentConversationEntry entry(String agentId, ToolExecution tool, GuardrailExecution guardrail) {
    var entry = new AgentConversationEntry();
    entry.setAgentId(agentId);
    entry.setToolExecutions(List.of(tool));
    entry.setGuardrailExecutions(List.of(guardrail));
    return entry;
  }
}
