package com.axonivy.utils.smart.workflow.guardrails.output;

import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class SensitiveDataOutputGuardrail implements SmartWorkflowOutputGuardrail {

  private static final String FAILURE_MESSAGE = "The AI response was blocked because it contains sensitive data such as API keys or private keys";

  private static final Pattern SK_KEY_PATTERN = Pattern.compile(
      "\\bsk-(?:[a-zA-Z0-9_]+-)*[a-zA-Z0-9_]{20,}\\b");

  private static final Pattern AWS_KEY_PATTERN = Pattern.compile(
      "\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");

  private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile(
      "\\bgh[posr]_[a-zA-Z0-9]{36}\\b|\\bgithub_pat_[a-zA-Z0-9_]{82}\\b");

  private static final Pattern GOOGLE_API_KEY_PATTERN = Pattern.compile(
      "\\bAIza[0-9A-Za-z_-]{35}\\b");

  private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
      "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");

  @Override
  public GuardrailResult evaluate(String message) {
    if (message == null || message.isBlank()) {
      return GuardrailResult.allow();
    }

    boolean containsSensitiveData = SK_KEY_PATTERN.matcher(message).find()
        || AWS_KEY_PATTERN.matcher(message).find()
        || GITHUB_TOKEN_PATTERN.matcher(message).find()
        || GOOGLE_API_KEY_PATTERN.matcher(message).find()
        || PRIVATE_KEY_PATTERN.matcher(message).find();

    return containsSensitiveData ? GuardrailResult.block(FAILURE_MESSAGE) : GuardrailResult.allow();
  }
}
