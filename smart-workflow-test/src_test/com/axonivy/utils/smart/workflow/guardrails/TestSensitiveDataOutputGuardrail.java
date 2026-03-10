package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestSensitiveDataOutputGuardrail {

  private SensitiveDataOutputGuardrail guardrail;

  @BeforeEach
  void setup() {
    guardrail = new SensitiveDataOutputGuardrail();
  }

  @Test
  void allowNormalResponse() {
    var result = guardrail.evaluate("Here is a summary of the quarterly report.");
    assertThat(result.isAllowed()).isTrue();
  }

  @Test
  void allowEmptyResponse() {
    var result = guardrail.evaluate("");
    assertThat(result.isAllowed()).isTrue();
  }

  @Test
  void allowNullResponse() {
    var result = guardrail.evaluate(null);
    assertThat(result.isAllowed()).isTrue();
  }

  // The following fake credentials are used for testing purposes only and do not provide
  // access to any production systems. Please do not submit them as part of a bug bounty program.
  @Test
  void blockOpenAIApiKey() {
    var result = guardrail.evaluate("Use this key: " + "sk-" + "abcdefghijklmnopqrstuvwxyz12345678");
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("sensitive data");
  }

  @Test
  void blockAWSAccessKey() {
    var result = guardrail.evaluate("AWS key: " + "AKIA" + "IOSFODNN7EXAMPLE");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGitHubToken() {
    var result = guardrail.evaluate("Token: " + "ghp_" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockPrivateKey() {
    var result = guardrail.evaluate("-----BEGIN RSA " + "PRIVATE KEY-----\nMIIEpA...\n-----END RSA " + "PRIVATE KEY-----");
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("sensitive data");
  }

  @Test
  void blockPrivateKeyGeneric() {
    var result = guardrail.evaluate("Here is the cert:\n" + "-----BEGIN " + "PRIVATE KEY-----\ndata\n" + "-----END " + "PRIVATE KEY-----");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void allowNormalNumbersInText() {
    var result = guardrail.evaluate("The project has 15000 users and 250 active sessions today.");
    assertThat(result.isAllowed()).isTrue();
  }
}
