package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public interface GuardrailProvider {
  List<SmartWorkflowInputGuardrail> getInputGuardrails();

  default String name() {
    return getClass().getSimpleName();
  }
}