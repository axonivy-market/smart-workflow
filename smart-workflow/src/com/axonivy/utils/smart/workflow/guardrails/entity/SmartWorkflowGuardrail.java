package com.axonivy.utils.smart.workflow.guardrails.entity;

public interface SmartWorkflowGuardrail {
  GuardrailResult evaluate(String message);

  default String name() {
    return getClass().getSimpleName();
  }
}
