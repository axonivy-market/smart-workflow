package com.axonivy.utils.smart.workflow.guardrails.output;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;

import ch.ivyteam.ivy.environment.Ivy;

public class SensitiveDataOutputGuardrail implements SmartWorkflowOutputGuardrail {

  private static final String FAILURE_MESSAGE = "The AI response was blocked because it contains sensitive data such as API keys or private keys";
  
  private interface Patterns {
    Pattern SK_KEY = Pattern.compile(
        "\\bsk-(?:[a-zA-Z0-9_]+-)*[a-zA-Z0-9_]{20,}\\b");
  
    Pattern AWS_KEY = Pattern.compile(
        "\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");
  
    Pattern GITHUB_TOKEN = Pattern.compile(
        "\\bgh[posr]_[a-zA-Z0-9]{36}\\b|\\bgithub_pat_[a-zA-Z0-9_]{82}\\b");
  
    Pattern GOOGLE_API_KEY = Pattern.compile(
        "\\bAIza[0-9A-Za-z_-]{35}\\b");
  
    Pattern XAI_KEY = Pattern.compile(
        "\\bxai-[a-zA-Z0-9]{50,}\\b");
  
    Pattern PRIVATE_KEY = Pattern.compile(
        "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");
  }

  private Set<String> variables = null;

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
    if (variables == null) {
      variables = loadConfiguredApiKeys();
    }
    return variables.stream().anyMatch(message::contains);
  }

  private static Set<String> loadConfiguredApiKeys() {
    var secrets = ChatModelFactory.providers().stream()
      .flatMap(provider -> provider.secretsVars().stream())
      .map(varName -> Ivy.var().get(varName))
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.toSet());
    return secrets;
  }

  private boolean containsPatternMatch(String message) {
    return Patterns.SK_KEY.matcher(message).find()
        || Patterns.AWS_KEY.matcher(message).find()
        || Patterns.GITHUB_TOKEN.matcher(message).find()
        || Patterns.GOOGLE_API_KEY.matcher(message).find()
        || Patterns.XAI_KEY.matcher(message).find()
        || Patterns.PRIVATE_KEY.matcher(message).find();
  }
}
