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
    var result = guardrail.evaluate("Use this key: " + "sk-proj-" + "AoFeTE4Mf4dtepe5Hab-6DdoLEdGd-JoEJGEGdb46mydDS6aaaaaaaaaaaaaaaaaaaaaaa-mG4JbyeDFYaaaaaaaaaaaaaa_6Guaaaaaaaaaaaaaa_YDJV6aaaaaaaaaaaaaaa_ugEY43aaaaaaaaaaaaa");
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("sensitive data");
  }

  @Test
  void blockAWSAccessKey() {
    var result = guardrail.evaluate("AWS key: " + "AKIA" + "IOSFODNN7EXAMPLE");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockAWSTemporaryAccessKey() {
    var result = guardrail.evaluate("AWS STS key: " + "ASIA" + "IOSFODNN7EXAMPLE");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGitHubToken() {
    var result = guardrail.evaluate("Token: " + "ghp_" + "ABCDEFGHIJKLMNOPabcdefghijklmnop0123");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGitHubOAuthToken() {
    var result = guardrail.evaluate("Token: " + "gho_" + "ABCDEFGHIJKLMNOPabcdefghijklmnop0123");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGitHubActionsToken() {
    var result = guardrail.evaluate("Token: " + "ghs_" + "ABCDEFGHIJKLMNOPabcdefghijklmnop0123");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGitHubFineGrainedPAT() {
    var result = guardrail.evaluate("Token: " + "github_pat_" + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRST");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockGoogleApiKey() {
    var result = guardrail.evaluate("Gemini key: " + "AIza" + "SyAetceEKUlfmeytmeWtU_UD6dU0Ueg4UEM");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockXAiApiKey() {
    var result = guardrail.evaluate("xAI key: " + "xai-" + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567890AB");
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void blockPrivateKey() {
    var result = guardrail.evaluate("""
        -----BEGIN RSA PRIVATE KEY-----
        MIIEpA...
        -----END RSA PRIVATE KEY-----
        """);
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("sensitive data");
  }

  @Test
  void blockPrivateKeyGeneric() {
    var result = guardrail.evaluate("""
        Here is the cert:
        -----BEGIN PRIVATE KEY-----
        data
        -----END PRIVATE KEY-----
        """);
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  void allowNormalNumbersInText() {
    var result = guardrail.evaluate("The project has 15000 users and 250 active sessions today.");
    assertThat(result.isAllowed()).isTrue();
  }
}
