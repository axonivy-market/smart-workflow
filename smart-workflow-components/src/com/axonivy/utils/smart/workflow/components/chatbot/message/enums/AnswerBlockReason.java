package com.axonivy.utils.smart.workflow.components.chatbot.message.enums;

/**
 * Why Smart Workflow refused to answer. Each reason maps to a user-facing CMS text; the raw error
 * message is not shown to users, as its format is not part of Smart Workflow's public contract.
 */
public enum AnswerBlockReason {
  /** {@code smartworkflow:guardrail:input:violation}: the question was blocked before it reached the AI. */
  INPUT_GUARDRAIL("InputGuardrailMessage"),
  /** {@code smartworkflow:guardrail:output:violation}: the AI's answer was blocked. */
  OUTPUT_GUARDRAIL("OutputGuardrailMessage"),
  /** {@code smartworkflow:stop}: the circuit breaker has switched off all AI calls in the application. */
  CIRCUIT_BREAKER("CircuitBreakerMessage");

  private final String cmsKey;

  AnswerBlockReason(String cmsKey) {
    this.cmsKey = cmsKey;
  }

  public String getCmsKey() {
    return cmsKey;
  }
}
