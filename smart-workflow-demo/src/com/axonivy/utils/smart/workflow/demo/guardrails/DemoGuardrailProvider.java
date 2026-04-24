package com.axonivy.utils.smart.workflow.demo.guardrails;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

/**
 * SPI provider that registers demo custom guardrails.
 * Discovered automatically via META-INF/services registration.
 */
public class DemoGuardrailProvider implements GuardrailProvider {

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new BlockCompetitorMentionGuardrail());
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of();
  }

  @Override
  public String name() {
    return "DemoGuardrailProvider";
  }
}
