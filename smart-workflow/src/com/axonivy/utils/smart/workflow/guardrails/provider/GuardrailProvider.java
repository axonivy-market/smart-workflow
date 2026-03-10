package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.Collections;
import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public interface GuardrailProvider {
  List<SmartWorkflowInputGuardrail> getInputGuardrails();

  default List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return Collections.emptyList();
  }

  default String name() {
    return getClass().getSimpleName();
  }
}