package com.axonivy.utils.smart.workflow.guardrails.output;

import java.util.Map;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingStore;

public class PiiMaskingOutputGuardrail implements SmartWorkflowOutputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.allow();
  }

  @Override
  public GuardrailResult evaluate(String message, String invocationId) {
    Map<String, String> mapping = PiiMaskingStore.getAndRemove(invocationId);
    if (mapping.isEmpty()) {
      return GuardrailResult.allow();
    }
    String restored = PiiDetector.restore(message, mapping);
    return restored.equals(message) ? GuardrailResult.allow() : GuardrailResult.allowWithRewrite(restored);
  }
}
