package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.Map;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector.MaskingResult;

/**
 * Masks Personally Identifiable Information (PII) in user messages before they
 * reach the LLM, then restores the original values in the AI response.
 *
 * <p>This guardrail implements both input and output phases. The input phase
 * masks detected PII and stores the mapping as instance state; the output phase
 * restores the originals from that mapping. Both phases must share the same
 * instance within a single agent invocation.
 */
public class PiiMaskingGuardrail implements SmartWorkflowInputGuardrail, SmartWorkflowOutputGuardrail {

  private Map<String, String> placeholderToOriginal;

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.allow();
  }

  @Override
  public GuardrailResult evaluate(String message, String invocationId) {
    if (invocationId == null) {
      return GuardrailResult.allow();
    }
    if (placeholderToOriginal == null) {
      return evaluateInput(message);
    }
    return evaluateOutput(message);
  }

  private GuardrailResult evaluateInput(String message) {
    MaskingResult result = PiiDetector.detectAndMask(message);
    placeholderToOriginal = result.hasPii() ? result.placeholderToOriginal() : Map.of();
    if (!result.hasPii()) {
      return GuardrailResult.allow();
    }
    return GuardrailResult.allowWithRewrite(result.maskedText());
  }

  private GuardrailResult evaluateOutput(String message) {
    if (placeholderToOriginal.isEmpty()) {
      return GuardrailResult.allow();
    }
    String restored = PiiDetector.restore(message, placeholderToOriginal);
    return restored.equals(message) ? GuardrailResult.allow() : GuardrailResult.allowWithRewrite(restored);
  }
}
