package com.axonivy.utils.smart.workflow.guardrails.dummy;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class DummyOutputGuardrail implements SmartWorkflowOutputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.block("Dummy output error");
  }
}
