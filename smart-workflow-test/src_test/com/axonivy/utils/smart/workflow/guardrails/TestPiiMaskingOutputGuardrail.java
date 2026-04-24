package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.output.PiiMaskingOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingStore;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestPiiMaskingOutputGuardrail {

  private PiiMaskingOutputGuardrail guardrail;

  @BeforeEach
  void setup() {
    guardrail = new PiiMaskingOutputGuardrail();
  }

  @Test
  void restoresPiiInLlmResponse() {
    var masked = PiiDetector.detectAndMask("user@example.com");
    PiiMaskingStore.put("output-test-1", masked.placeholderToOriginal());
    String llmResponse = "I will contact " + masked.maskedText() + " about your inquiry.";
    var result = guardrail.evaluate(llmResponse, "output-test-1");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isTrue();
    assertThat(result.getRewrittenMessage()).contains("user@example.com");
    assertThat(result.getRewrittenMessage()).doesNotContain("<EMAIL_");
  }

  @Test
  void passthroughWhenNoMappingStored() {
    var result = guardrail.evaluate("Normal response text.", "output-test-no-mapping");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }

  @Test
  void passthroughWhenPlaceholderAbsentFromResponse() {
    var masked = PiiDetector.detectAndMask("user@example.com");
    PiiMaskingStore.put("output-test-2", masked.placeholderToOriginal());
    var result = guardrail.evaluate("I'll help you with that request.", "output-test-2");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }

  @Test
  void allowWhenNoInvocationId() {
    var result = guardrail.evaluate("Some response text.");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }
}
