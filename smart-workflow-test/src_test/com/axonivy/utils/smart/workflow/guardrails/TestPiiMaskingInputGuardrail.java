package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.input.PiiMaskingInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingStore;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

/**
 * Dear Bug Hunter,
 * The credentials and PII values used in these tests are intentionally included for testing purposes only
 * and do not provide access to any production systems or real persons.
 * Please do not submit them as part of our bug bounty program.
 */
@IvyProcessTest
public class TestPiiMaskingInputGuardrail {

  private PiiMaskingInputGuardrail guardrail;

  @BeforeEach
  void setup() {
    guardrail = new PiiMaskingInputGuardrail();
  }

  @Test
  void allowNormalMessage() {
    var result = guardrail.evaluate("What is the weather today?", "input-test-1");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }

  @Test
  void maskEmailAndAllowWithRewrite() {
    var result = guardrail.evaluate("My email is user@example.com", "input-test-2");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isTrue();
    assertThat(result.getRewrittenMessage()).doesNotContain("user@example.com");
    assertThat(result.getRewrittenMessage()).contains("<EMAIL_");
  }

  @Test
  void maskCreditCardAndAllowWithRewrite() {
    var result = guardrail.evaluate("Pay with card 4532015112830366", "input-test-3");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isTrue();
    assertThat(result.getRewrittenMessage()).doesNotContain("4532015112830366");
    assertThat(result.getRewrittenMessage()).contains("<CREDIT_DEBIT_CARD_NUMBER_");
  }

  @Test
  void storeMappingForOutputGuardrail() {
    guardrail.evaluate("Server: 192.168.1.1", "input-test-4");
    var mapping = PiiMaskingStore.getAndRemove("input-test-4");
    assertThat(mapping).isNotEmpty();
    assertThat(mapping).containsValue("192.168.1.1");
  }


  @Test
  void allowWhenNoInvocationId() {
    // Backward compatibility: evaluate(String) must not rewrite
    var result = guardrail.evaluate("My email is user@example.com");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }

  @Test
  void allowNullMessage() {
    var result = guardrail.evaluate(null, "input-test-5");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.hasRewrite()).isFalse();
  }
}
