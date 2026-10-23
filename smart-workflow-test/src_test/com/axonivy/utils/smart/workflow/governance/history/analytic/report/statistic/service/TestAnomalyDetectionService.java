package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionContext;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyDetectionOptions;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyType;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.anomaly.AnomalyIssue;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAnomalyDetectionService {

  private final AnomalyDetectionService service = new AnomalyDetectionService();

  @Test
  void detect_noConditionsTriggered_returnsEmpty() {
    assertThat(detect(List.of(), List.of(), List.of(), 100, 1_000, "STOP")).isEmpty();
  }

  @Test
  void detect_durationExceedsThreshold_anomalyDetected() {
    var result = detect(List.of(), List.of(), List.of(), 100, 31_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).containsExactly(31_000L);
    assertThat(result.get(0).type()).isEqualTo(AnomalyType.EXCESSIVE_DURATION);
  }

  @Test
  void getMessage_formatsEnumTemplateAndArguments() {
    var issue = detect(List.of(), List.of(), List.of(), 100, 31_000, "STOP").get(0);

    assertThat(issue.getMessage())
        .isEqualTo("Duration exceeded 30s (31000ms)");
  }

  @Test
  void detect_durationAtThreshold_noAnomaly() {
    assertThat(detect(List.of(), List.of(), List.of(), 100, 30_000, "STOP")).isEmpty();
  }

  @Test
  void detect_tokensExceedThreshold_anomalyDetected() {
    var result = detect(List.of(), List.of(), List.of(), 10_001, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).containsExactly(10_001);
  }

  @Test
  void detect_toolCallCountExceedsThreshold_anomalyDetected() {
    var tools = repeat(toolExecution("search", "{}", "ok"), 11);
    var result = detect(tools, List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(2);
    assertThat(result).anySatisfy(issue -> {
      assertThat(issue.type()).isEqualTo(AnomalyType.EXCESSIVE_TOOL_CALLS);
      assertThat(issue.messageArguments()).containsExactly(10, 11);
    });
    assertThat(result).anySatisfy(issue -> {
      assertThat(issue.type()).isEqualTo(AnomalyType.STUCK_LOOP);
      assertThat(issue.messageArguments()).containsExactly("search", 11L);
    });
  }

  @Test
  void detect_fatalGuardrailResult_anomalyDetected() {
    var guardrail = new GuardrailSummary("pii-check", 5, 0, 1, 0);
    var result = detect(List.of(), List.of(), List.of(guardrail), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).containsExactly("pii-check", 1);
  }

  @Test
  void detect_guardrailWithZeroFatals_noAnomaly() {
    var guardrail = new GuardrailSummary("pii-check", 5, 2, 0, 0);
    assertThat(detect(List.of(), List.of(), List.of(guardrail), 100, 1_000, "STOP")).isEmpty();
  }

  @Test
  void detect_lengthFinishReason_anomalyDetected() {
    var result = detect(List.of(), List.of(), List.of(), 100, 1_000, "LENGTH");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).isEmpty();
  }

  @Test
  void detect_highNullResultRate_anomalyDetected() {
    var toolSummary = new ToolSummary("extractor", 10, 6, 0, null);
    var result = detect(List.of(), List.of(toolSummary), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).containsExactly("extractor", 6, 10);
  }

  @Test
  void detect_lowNullResultRate_noAnomaly() {
    var toolSummary = new ToolSummary("extractor", 10, 4, 0, null);
    assertThat(detect(List.of(), List.of(toolSummary), List.of(), 100, 1_000, "STOP")).isEmpty();
  }

  @Test
  void detect_stuckLoop_anomalyDetected() {
    var tools = repeat(toolExecution("processItem", "{\"id\":1}", null), 6);
    var result = detect(tools, List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).messageArguments()).containsExactly("processItem", 6L);
  }

  @Test
  void detect_stuckLoopAtThreshold_noAnomaly() {
    var tools = repeat(toolExecution("processItem", "{\"id\":1}", null), 5);
    assertThat(detect(tools, List.of(), List.of(), 100, 1_000, "STOP")).isEmpty();
  }

  @Test
  void detect_onlySelectedDetector_doesNotLoadUnusedStatistics() {
    var toolSourceCalls = new AtomicInteger();
    var context = context(List.of(), List.of(), List.of(), 100, 31_000, "STOP",
        () -> {
          toolSourceCalls.incrementAndGet();
          return List.of();
        });

    var result = service.detect(context,
        new AnomalyDetectionOptions(Set.of(AnomalyType.EXCESSIVE_DURATION)));

    assertThat(result).hasSize(1);
    assertThat(toolSourceCalls).hasValue(0);
  }

  @Test
  void detect_nullInputLists_handledGracefully() {
    var context = context(null, null, null, 100, 1_000, "STOP", () -> null);
    assertThat(service.detect(context, AnomalyDetectionOptions.defaults())).isEmpty();
  }

  private List<AnomalyIssue> detect(List<ToolExecution> tools, List<ToolSummary> toolSummaries,
      List<GuardrailSummary> guardrailSummaries, int totalTokens, long durationMs, String finishReason) {
    var context = context(tools, toolSummaries, guardrailSummaries, totalTokens, durationMs, finishReason, () -> tools);
    return service.detect(context, AnomalyDetectionOptions.defaults());
  }

  private static AnomalyDetectionContext context(List<ToolExecution> tools, List<ToolSummary> toolSummaries,
      List<GuardrailSummary> guardrailSummaries, int totalTokens, long durationMs, String finishReason,
      Supplier<List<ToolExecution>> toolExecutionsSource) {
    var summary = new AgentSummary();
    summary.setTotalTokens(totalTokens);
    summary.setDurationMs(durationMs);
    summary.setFinishReason(finishReason);
    summary.setToolCallCount(tools == null ? 0 : tools.size());
    summary.setToolSummaries(toolSummaries);
    summary.setGuardrailSummaries(guardrailSummaries);
    return new AnomalyDetectionContext(summary, toolExecutionsSource);
  }

  private static ToolExecution toolExecution(String toolName, String arguments, String resultText) {
    return new ToolExecution(toolName, arguments, resultText, null);
  }

  private static List<ToolExecution> repeat(ToolExecution execution, int count) {
    return Collections.nCopies(count, execution);
  }
}
