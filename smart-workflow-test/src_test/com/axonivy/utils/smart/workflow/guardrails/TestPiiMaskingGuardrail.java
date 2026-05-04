package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingGuardrail;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

/**
 * Dear Bug Hunter,
 * The credentials and PII values used in these tests are intentionally included for testing purposes only
 * and do not provide access to any production systems or real persons.
 * Please do not submit them as part of our bug bounty program.
 */
@IvyProcessTest
public class TestPiiMaskingGuardrail {

  private PiiMaskingGuardrail guardrail;

  @BeforeEach
  void setup() {
    guardrail = new PiiMaskingGuardrail();
  }

  @Test
  void allowNormalMessage() {
    var result = guardrail.evaluate("What is the weather today?", "inv-1");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRewrittenMessage()).isEmpty();
  }

  @Test
  void maskEmailAndAllowWithRewrite() {
    var result = guardrail.evaluate("My email is user@example.com", "inv-2");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRewrittenMessage()).isPresent();
    assertThat(result.getRewrittenMessage().get()).doesNotContain("user@example.com");
    assertThat(result.getRewrittenMessage().get()).contains("<EMAIL_");
  }

  @Test
  void maskCreditCardAndAllowWithRewrite() {
    var result = guardrail.evaluate("Pay with card 4532015112830366", "inv-3");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRewrittenMessage()).isPresent();
    assertThat(result.getRewrittenMessage().get()).doesNotContain("4532015112830366");
    assertThat(result.getRewrittenMessage().get()).contains("<CREDIT_DEBIT_CARD_NUMBER_");
  }

  @Test
  void roundTripRestoresPii() {
    var inputResult = guardrail.evaluate("My email is user@example.com", "inv-4");
    assertThat(inputResult.getRewrittenMessage()).isPresent();
    String maskedMessage = inputResult.getRewrittenMessage().get();

    String llmResponse = "I will contact " + maskedMessage.replace("My email is ", "") + " about your inquiry.";
    var outputResult = guardrail.evaluate(llmResponse, "inv-4");
    assertThat(outputResult.isAllowed()).isTrue();
    assertThat(outputResult.getRewrittenMessage()).isPresent();
    assertThat(outputResult.getRewrittenMessage().get()).contains("user@example.com");
    assertThat(outputResult.getRewrittenMessage().get()).doesNotContain("<EMAIL_");
  }

  @Test
  void passthroughOutputWhenNoPiiInInput() {
    guardrail.evaluate("What is the weather today?", "inv-5");
    var outputResult = guardrail.evaluate("It will be sunny.", "inv-5");
    assertThat(outputResult.isAllowed()).isTrue();
    assertThat(outputResult.getRewrittenMessage()).isEmpty();
  }

  @Test
  void passthroughOutputWhenPlaceholderAbsentFromResponse() {
    guardrail.evaluate("Server: 192.168.1.1", "inv-6");
    var outputResult = guardrail.evaluate("I'll help you with that request.", "inv-6");
    assertThat(outputResult.isAllowed()).isTrue();
    assertThat(outputResult.getRewrittenMessage()).isEmpty();
  }

  @Test
  void allowWhenNoInvocationId() {
    var result = guardrail.evaluate("My email is user@example.com");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRewrittenMessage()).isEmpty();
  }

  @Test
  void allowNullMessage() {
    var result = guardrail.evaluate(null, "inv-7");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRewrittenMessage()).isEmpty();
  }
}
