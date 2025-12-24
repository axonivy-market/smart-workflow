package com.axonivy.utils.smart.workflow.guardrails.entity;

public interface SmartWorkflowInputGuardrail {
  String name();
  GuardrailResult evaluate(String message);

  default String getDisplayName() {
    return String.format("%s (%s)", name(), this.getClass().getSimpleName());
  }
}
