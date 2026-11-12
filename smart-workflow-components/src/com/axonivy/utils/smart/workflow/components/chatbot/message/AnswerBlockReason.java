package com.axonivy.utils.smart.workflow.components.chatbot.message;

public enum AnswerBlockReason {
  INPUT_GUARDRAIL("InputGuardrailMessage"),
  OUTPUT_GUARDRAIL("OutputGuardrailMessage"),
  CIRCUIT_BREAKER("CircuitBreakerMessage");

  private final String cmsKey;

  AnswerBlockReason(String cmsKey) {
    this.cmsKey = cmsKey;
  }

  public String getCmsKey() {
    return cmsKey;
  }
}
