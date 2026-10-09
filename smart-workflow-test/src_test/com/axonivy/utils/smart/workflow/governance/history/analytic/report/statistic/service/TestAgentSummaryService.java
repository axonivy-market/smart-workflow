package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentSummaryService {

  // ── summarizeTools ──────────────────────────────────────────────────────────

  @Test
  void summarizeTools_singleTool_correctCallCount() {
    var entry = entryWithTools(List.of(
        new ToolExecution("search", "{}", "result", null),
        new ToolExecution("search", "{}", "result", null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).getToolName()).isEqualTo("search");
    assertThat(summaries.get(0).getCallCount()).isEqualTo(2);
  }

  @Test
  void summarizeTools_multipleTools_groupedByName() {
    var entry = entryWithTools(List.of(
        new ToolExecution("search", "{}", "result", null),
        new ToolExecution("fetch",  "{}", "result", null),
        new ToolExecution("search", "{}", "result", null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries).hasSize(2);
    var names = summaries.stream().map(s -> s.getToolName()).toList();
    assertThat(names).containsExactlyInAnyOrder("search", "fetch");
  }

  @Test
  void summarizeTools_nullOrBlankResult_countedAsNullResult() {
    var entry = entryWithTools(List.of(
        new ToolExecution("search", "{}", null,  null),
        new ToolExecution("search", "{}", "   ", null),
        new ToolExecution("search", "{}", "ok",  null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries.get(0).getNullResultCount()).isEqualTo(2);
  }

  @Test
  void summarizeTools_resultStartsWithError_countedAsError() {
    var entry = entryWithTools(List.of(
        new ToolExecution("fetch", "{}", "Error: timeout", null),
        new ToolExecution("fetch", "{}", "error occurred", null),
        new ToolExecution("fetch", "{}", "ok result",     null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries.get(0).getErrorCount()).isEqualTo(2);
  }

  @Test
  void summarizeTools_firstNonBlankArguments_usedAsSample() {
    var entry = entryWithTools(List.of(
        new ToolExecution("search", null,       "r", null),
        new ToolExecution("search", "{\"q\":1}", "r", null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries.get(0).getSampleArguments()).isEqualTo("{\"q\":1}");
  }

  @Test
  void summarizeTools_multipleEntries_toolsAggregatedAcrossEntries() {
    var e1 = entryWithTools(List.of(new ToolExecution("search", "{}", "r", null)));
    var e2 = entryWithTools(List.of(new ToolExecution("search", "{}", "r", null)));
    var summaries = AgentSummaryService.summarizeTools(List.of(e1, e2));
    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).getCallCount()).isEqualTo(2);
  }

  @Test
  void summarizeTools_emptyEntries_returnsEmpty() {
    var summaries = AgentSummaryService.summarizeTools(List.of());
    assertThat(summaries).isEmpty();
  }

  @Test
  void summarizeTools_entryWithNoTools_returnsEmpty() {
    var entry = new AgentConversationEntry();
    var summaries = AgentSummaryService.summarizeTools(List.of(entry));
    assertThat(summaries).isEmpty();
  }

  // ── summarizeGuardrails ─────────────────────────────────────────────────────

  @Test
  void summarizeGuardrails_passedResult_countedAsPassed() {
    var entry = entryWithGuardrails(List.of(
        guardrail("pii-check", "PASSED", null),
        guardrail("pii-check", "SUCCESS", null)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).getPassedCount()).isEqualTo(2);
  }

  @Test
  void summarizeGuardrails_failedResult_countedAsFailed() {
    var entry = entryWithGuardrails(List.of(
        guardrail("content-filter", "FAILED", null)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries.get(0).getFailedCount()).isEqualTo(1);
    assertThat(summaries.get(0).getFatalCount()).isEqualTo(0);
  }

  @Test
  void summarizeGuardrails_fatalResult_countedAsFatal() {
    var entry = entryWithGuardrails(List.of(
        guardrail("safety-check", "FATAL", null)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries.get(0).getFatalCount()).isEqualTo(1);
    assertThat(summaries.get(0).getFailedCount()).isEqualTo(0);
  }

  @Test
  void summarizeGuardrails_mixedResults_correctCounts() {
    var entry = entryWithGuardrails(List.of(
        guardrail("g", "PASSED", null),
        guardrail("g", "FAILED", null),
        guardrail("g", "FATAL",  null),
        guardrail("g", "PASSED", null)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries.get(0).getPassedCount()).isEqualTo(2);
    assertThat(summaries.get(0).getFailedCount()).isEqualTo(1);
    assertThat(summaries.get(0).getFatalCount()).isEqualTo(1);
  }

  @Test
  void summarizeGuardrails_avgDurationMs_computedFromEntries() {
    var entry = entryWithGuardrails(List.of(
        guardrail("g", "PASSED", 100L),
        guardrail("g", "PASSED", 200L)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries.get(0).getAvgDurationMs()).isEqualTo(150.0);
  }

  @Test
  void summarizeGuardrails_multipleGuardrailTypes_groupedByName() {
    var entry = entryWithGuardrails(List.of(
        guardrail("pii-check",      "PASSED", null),
        guardrail("content-filter", "FAILED", null)));
    var summaries = AgentSummaryService.summarizeGuardrails(List.of(entry));
    assertThat(summaries).hasSize(2);
  }

  // ── summarizeTokenUsage ─────────────────────────────────────────────────────

  @Test
  void summarizeTokenUsage_singleEntry_correctAggregates() {
    var entry = entryWithTokenUsage(
        "[{\"inputTokens\":100,\"outputTokens\":50,\"totalTokens\":150,\"durationMs\":1000,\"finishReason\":\"STOP\",\"modelName\":\"gpt-4\"}]");
    var summary = AgentSummaryService.summarizeTokenUsage(List.of(entry));
    assertThat(summary.getTotalInputTokens()).isEqualTo(100);
    assertThat(summary.getTotalOutputTokens()).isEqualTo(50);
    assertThat(summary.getTotalTokens()).isEqualTo(150);
    assertThat(summary.getMaxSingleConversationTokens()).isEqualTo(150);
  }

  @Test
  void summarizeTokenUsage_multipleEntries_sumsAllTokens() {
    var e1 = entryWithTokenUsage(
        "[{\"inputTokens\":100,\"outputTokens\":50,\"totalTokens\":150,\"durationMs\":500,\"finishReason\":\"STOP\",\"modelName\":\"m\"}]");
    var e2 = entryWithTokenUsage(
        "[{\"inputTokens\":200,\"outputTokens\":100,\"totalTokens\":300,\"durationMs\":500,\"finishReason\":\"STOP\",\"modelName\":\"m\"}]");
    var summary = AgentSummaryService.summarizeTokenUsage(List.of(e1, e2));
    assertThat(summary.getTotalInputTokens()).isEqualTo(300);
    assertThat(summary.getTotalOutputTokens()).isEqualTo(150);
    assertThat(summary.getTotalTokens()).isEqualTo(450);
  }

  @Test
  void summarizeTokenUsage_maxSingle_isHighestPerConversation() {
    var e1 = entryWithTokenUsage(
        "[{\"inputTokens\":50,\"outputTokens\":50,\"totalTokens\":100,\"durationMs\":500,\"finishReason\":\"STOP\",\"modelName\":\"m\"}]");
    var e2 = entryWithTokenUsage(
        "[{\"inputTokens\":100,\"outputTokens\":200,\"totalTokens\":300,\"durationMs\":500,\"finishReason\":\"STOP\",\"modelName\":\"m\"}]");
    var summary = AgentSummaryService.summarizeTokenUsage(List.of(e1, e2));
    assertThat(summary.getMaxSingleConversationTokens()).isEqualTo(300);
  }

  @Test
  void summarizeTokenUsage_emptyEntries_returnsAllZeros() {
    var summary = AgentSummaryService.summarizeTokenUsage(List.of());
    assertThat(summary.getTotalTokens()).isEqualTo(0);
    assertThat(summary.getTotalInputTokens()).isEqualTo(0);
    assertThat(summary.getTotalOutputTokens()).isEqualTo(0);
    assertThat(summary.getMaxSingleConversationTokens()).isEqualTo(0);
  }

  @Test
  void summarizeTokenUsage_nullTokenUsageJson_handledGracefully() {
    var entry = new AgentConversationEntry();
    var summary = AgentSummaryService.summarizeTokenUsage(List.of(entry));
    assertThat(summary.getTotalTokens()).isEqualTo(0);
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  private static AgentConversationEntry entryWithTools(List<ToolExecution> tools) {
    var entry = new AgentConversationEntry();
    entry.setToolExecutions(tools);
    return entry;
  }

  private static AgentConversationEntry entryWithGuardrails(List<GuardrailExecution> guardrails) {
    var entry = new AgentConversationEntry();
    entry.setGuardrailExecutions(guardrails);
    return entry;
  }

  private static AgentConversationEntry entryWithTokenUsage(String json) {
    var entry = new AgentConversationEntry();
    entry.setTokenUsageJson(json);
    return entry;
  }

  private static GuardrailExecution guardrail(String name, String result, Long durationMs) {
    return new GuardrailExecution(name, null, result, null, null, durationMs, null);
  }
}
