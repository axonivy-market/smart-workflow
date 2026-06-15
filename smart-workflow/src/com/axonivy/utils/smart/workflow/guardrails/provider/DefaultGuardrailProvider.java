package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.AiPromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingGuardrail;

public class DefaultGuardrailProvider implements GuardrailProvider {

  private final PiiMaskingGuardrail piiMaskingGuardrail = new PiiMaskingGuardrail();

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new PromptInjectionInputGuardrail(), new AiPromptInjectionInputGuardrail(), piiMaskingGuardrail);
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of(new SensitiveDataOutputGuardrail(), piiMaskingGuardrail);
  }
}
