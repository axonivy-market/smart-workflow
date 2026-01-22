package com.axonivy.utils.smart.workflow.guardrails.entity;

public interface SmartWorkflowInputGuardrail {
  GuardrailResult evaluate(String message);

  default String name() {
    return getClass().getSimpleName();
  }
}
