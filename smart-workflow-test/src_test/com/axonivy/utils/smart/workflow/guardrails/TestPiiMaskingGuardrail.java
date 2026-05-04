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

  @Test
  void noPiiInputThenPiiInputAreIndependent() {
    // First invocation: no PII — must not poison state for next invocation
    guardrail.evaluate("What is the weather today?", "inv-8");
    guardrail.evaluate("It will be sunny.", "inv-8");

    // Second invocation on same instance: input with PII must still be masked
    var inputResult = guardrail.evaluate("My email is user@example.com", "inv-9");
    assertThat(inputResult.getRewrittenMessage()).isPresent();
    assertThat(inputResult.getRewrittenMessage().get()).contains("<EMAIL_");
  }

  @Test
  void mappingClearedAfterOutputPhase() {
    // Input phase stores mapping; output phase must remove it
    var inputResult = guardrail.evaluate("IP: 192.168.1.1", "inv-10");
    assertThat(inputResult.getRewrittenMessage()).isPresent();
    String maskedIp = inputResult.getRewrittenMessage().get();
    guardrail.evaluate("The server is at " + maskedIp, "inv-10");

    // A third call with the same invocationId must start a fresh input phase
    var result = guardrail.evaluate("New message: 192.168.1.1", "inv-10");
    assertThat(result.getRewrittenMessage()).isPresent();
    assertThat(result.getRewrittenMessage().get()).contains("<IP_ADDRESS_");
  }

  @Test
  void concurrentInvocationsAreIndependent() {
    var r1 = guardrail.evaluate("Email: user@example.com", "inv-A");
    var r2 = guardrail.evaluate("Call me at +1 555 123 4567", "inv-B");

    assertThat(r1.getRewrittenMessage()).isPresent();
    assertThat(r2.getRewrittenMessage()).isPresent();
    String maskedEmail = r1.getRewrittenMessage().get();
    assertThat(maskedEmail).contains("<EMAIL_");

    // Output for inv-A must restore its own email, not inv-B's phone
    var out1 = guardrail.evaluate("Contact " + maskedEmail, "inv-A");
    assertThat(out1.getRewrittenMessage()).isPresent();
    assertThat(out1.getRewrittenMessage().get()).contains("user@example.com");
    assertThat(out1.getRewrittenMessage().get()).doesNotContain("+1");
  }
}
