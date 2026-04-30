package com.axonivy.utils.smart.workflow.guardrails.entity;

public interface SmartWorkflowGuardrail {
  GuardrailResult evaluate(String message);

  default GuardrailResult evaluate(String message, String invocationId) {
    return evaluate(message);
  }

  default String name() {
    return getClass().getSimpleName();
  }
}
