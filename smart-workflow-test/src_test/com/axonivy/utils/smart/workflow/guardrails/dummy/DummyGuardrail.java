package com.axonivy.utils.smart.workflow.guardrails.dummy;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class DummyGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public String name() {
    return "Dummy Guardrail";
  }

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.block("Dummy error");
  }

}
