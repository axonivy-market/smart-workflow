package com.axonivy.utils.smart.workflow.guardrails.input;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector.MaskingResult;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiMaskingStore;

import ch.ivyteam.ivy.environment.Ivy;

public class PiiMaskingInputGuardrail implements SmartWorkflowInputGuardrail {

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.allow();
  }

  @Override
  public GuardrailResult evaluate(String message, String invocationId) {
    if (invocationId == null) {
      return GuardrailResult.allow();
    }
    MaskingResult result = PiiDetector.detectAndMask(message);
    if (!result.hasPii()) {
      return GuardrailResult.allow();
    }
    result.countByType().forEach((type, count) ->
        Ivy.log().info("[PII] type=" + type + " action=MASK count=" + count));
    PiiMaskingStore.put(invocationId, result.placeholderToOriginal());
    return GuardrailResult.allowWithRewrite(result.maskedText());
  }
}
