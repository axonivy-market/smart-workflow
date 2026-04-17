package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;

public class DefaultGuardrailProvider implements GuardrailProvider {
  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new PromptInjectionInputGuardrail());
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of(new SensitiveDataOutputGuardrail());
  }
}
