package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.GuardrailSummary;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.ToolSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAnomalyDetectionService {

  @Test
  void detect_noConditionsTriggered_returnsEmpty() {
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).isEmpty();
  }

  @Test
  void detect_durationExceedsThreshold_anomalyDetected() {
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(), 100, 31_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("31000ms");
  }

  @Test
  void detect_durationAtThreshold_noAnomaly() {
    // threshold is strictly greater than 30,000ms
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(), 100, 30_000, "STOP");
    assertThat(result).isEmpty();
  }

  @Test
  void detect_tokensExceedThreshold_anomalyDetected() {
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(), 10_001, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("10001");
  }

  @Test
  void detect_toolCallCountExceedsThreshold_anomalyDetected() {
    var tools = repeat(toolExecution("search", "{}", "ok"), 11);
    var result = AnomalyDetectionService.detect(tools, List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("11");
  }

  @Test
  void detect_fatalGuardrailResult_anomalyDetected() {
    var guardrail = new GuardrailSummary("pii-check", 5, 0, 1, 0);
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(guardrail), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("pii-check");
  }

  @Test
  void detect_guardrailWithZeroFatals_noAnomaly() {
    var guardrail = new GuardrailSummary("pii-check", 5, 2, 0, 0);
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(guardrail), 100, 1_000, "STOP");
    assertThat(result).isEmpty();
  }

  @Test
  void detect_lengthFinishReason_anomalyDetected() {
    var result = AnomalyDetectionService.detect(List.of(), List.of(), List.of(), 100, 1_000, "LENGTH");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).containsIgnoringCase("LENGTH");
  }

  @Test
  void detect_highNullResultRate_anomalyDetected() {
    // 6 nulls out of 10 calls = 60% > 50%
    var toolSummary = new ToolSummary("extractor", 10, 6, 0, null);
    var result = AnomalyDetectionService.detect(List.of(), List.of(toolSummary), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("extractor");
  }

  @Test
  void detect_lowNullResultRate_noAnomaly() {
    // 4 nulls out of 10 calls = 40% ≤ 50%
    var toolSummary = new ToolSummary("extractor", 10, 4, 0, null);
    var result = AnomalyDetectionService.detect(List.of(), List.of(toolSummary), List.of(), 100, 1_000, "STOP");
    assertThat(result).isEmpty();
  }

  @Test
  void detect_stuckLoop_anomalyDetected() {
    // same tool + same args called 6 times → > 5 threshold
    var tools = repeat(toolExecution("processItem", "{\"id\":1}", null), 6);
    var result = AnomalyDetectionService.detect(tools, List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).contains("processItem");
  }

  @Test
  void detect_stuckLoopAtThreshold_noAnomaly() {
    // same tool + args called exactly 5 times → not > 5, no anomaly
    var tools = repeat(toolExecution("processItem", "{\"id\":1}", null), 5);
    var result = AnomalyDetectionService.detect(tools, List.of(), List.of(), 100, 1_000, "STOP");
    assertThat(result).isEmpty();
  }

  @Test
  void detect_nullInputLists_handledGracefully() {
    var result = AnomalyDetectionService.detect(null, null, null, 100, 1_000, "STOP");
    assertThat(result).isEmpty();
  }

  private static ToolExecution toolExecution(String toolName, String arguments, String resultText) {
    return new ToolExecution(toolName, arguments, resultText, null);
  }

  private static List<ToolExecution> repeat(ToolExecution execution, int count) {
    return Collections.nCopies(count, execution);
  }
}
