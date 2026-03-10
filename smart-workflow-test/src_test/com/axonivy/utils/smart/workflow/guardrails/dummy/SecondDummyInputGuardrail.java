package com.axonivy.utils.smart.workflow.guardrails.dummy;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

public class SecondDummyInputGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.block("Second dummy input error");
  }

}
