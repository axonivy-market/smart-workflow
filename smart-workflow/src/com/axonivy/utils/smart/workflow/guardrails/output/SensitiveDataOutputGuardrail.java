package com.axonivy.utils.smart.workflow.guardrails.output;

import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class SensitiveDataOutputGuardrail implements SmartWorkflowOutputGuardrail {

  private static final String FAILURE_MESSAGE = "The AI response was blocked because it contains sensitive data such as API keys or private keys";

  // API Keys / Secrets: common patterns like "sk-...", "AKIA...", "ghp_..."
  private static final Pattern API_KEY_PATTERN = Pattern.compile(
      "(?i)\\b(sk-[a-zA-Z0-9]{20,}|AKIA[0-9A-Z]{16}|ghp_[a-zA-Z0-9]{36}|gho_[a-zA-Z0-9]{36})\\b");

  // Private key blocks
  private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
      "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");

  @Override
  public GuardrailResult evaluate(String message) {
    if (message == null || message.isBlank()) {
      return GuardrailResult.allow();
    }

    boolean containsSensitiveData = API_KEY_PATTERN.matcher(message).find()
        || PRIVATE_KEY_PATTERN.matcher(message).find();

    return containsSensitiveData ? GuardrailResult.block(FAILURE_MESSAGE) : GuardrailResult.allow();
  }
}
