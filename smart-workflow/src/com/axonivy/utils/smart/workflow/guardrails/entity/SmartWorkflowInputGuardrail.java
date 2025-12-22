package com.axonivy.utils.smart.workflow.guardrails.entity;

public interface SmartWorkflowInputGuardrail {
  String name();
  GuardrailResult evaluate(String message);
}
