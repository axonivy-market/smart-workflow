package com.axonivy.utils.smart.workflow.guardrails.output;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

import ch.ivyteam.ivy.environment.Ivy;

public class SensitiveDataOutputGuardrail implements SmartWorkflowOutputGuardrail {

  private static final String FAILURE_MESSAGE = "The AI response was blocked because it contains sensitive data such as API keys or private keys";

  private static final List<String> API_KEY_VARIABLE_PATHS = List.of(
      "Ai.Providers.OpenAI.APIKey",
      "AI.Providers.AzureOpenAI.Deployment.APIKey",
      "AI.Providers.Gemini.APIKey",
      "AI.Providers.xAI.APIKey",
      "AI.Providers.Anthropic.APIKey"
  );
  
  private static final Pattern SK_KEY_PATTERN = Pattern.compile(
      "\\bsk-(?:[a-zA-Z0-9_]+-)*[a-zA-Z0-9_]{20,}\\b");

  private static final Pattern AWS_KEY_PATTERN = Pattern.compile(
      "\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");

  private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile(
      "\\bgh[posr]_[a-zA-Z0-9]{36}\\b|\\bgithub_pat_[a-zA-Z0-9_]{82}\\b");

  private static final Pattern GOOGLE_API_KEY_PATTERN = Pattern.compile(
      "\\bAIza[0-9A-Za-z_-]{35}\\b");

  private static final Pattern XAI_KEY_PATTERN = Pattern.compile(
      "\\bxai-[a-zA-Z0-9]{50,}\\b");

  private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
      "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");

  @Override
  public GuardrailResult evaluate(String message) {
    if (message == null || message.isBlank()) {
      return GuardrailResult.allow();
    }

    if (containsConfiguredApiKey(message) || containsPatternMatch(message)) {
      return GuardrailResult.block(FAILURE_MESSAGE);
    }
    return GuardrailResult.allow();
  }

  private boolean containsConfiguredApiKey(String message) {
    return loadConfiguredApiKeys().stream().anyMatch(message::contains);
  }

  private static Set<String> loadConfiguredApiKeys() {
    return API_KEY_VARIABLE_PATHS.stream()
        .map(path -> Ivy.var().get(path))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());
  }

  private boolean containsPatternMatch(String message) {
    return SK_KEY_PATTERN.matcher(message).find()
        || AWS_KEY_PATTERN.matcher(message).find()
        || GITHUB_TOKEN_PATTERN.matcher(message).find()
        || GOOGLE_API_KEY_PATTERN.matcher(message).find()
        || XAI_KEY_PATTERN.matcher(message).find()
        || PRIVATE_KEY_PATTERN.matcher(message).find();
  }
}
