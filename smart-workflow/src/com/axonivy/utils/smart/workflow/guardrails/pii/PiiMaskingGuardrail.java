package com.axonivy.utils.smart.workflow.guardrails.pii;

import java.util.HashMap;
import java.util.Map;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.pii.PiiDetector.MaskingResult;

/**
 * Masks Personally Identifiable Information (PII) in user messages before they
 * reach the LLM, then restores the original values in the AI response.
 *
 * <p>Phase detection is explicit: the input phase stores a placeholder→original
 * mapping keyed by {@code invocationId}; the output phase looks up that mapping
 * and removes it after restoring, preventing stale PII retention across calls.
 */
public class PiiMaskingGuardrail implements SmartWorkflowInputGuardrail, SmartWorkflowOutputGuardrail {

  private final Map<String, Map<String, String>> inputMappings = new HashMap<>();

  @Override
  public GuardrailResult evaluate(String message) {
    return GuardrailResult.allow();
  }

  @Override
  public GuardrailResult evaluate(String message, String invocationId) {
    if (invocationId == null) {
      return GuardrailResult.allow();
    }
    if (!inputMappings.containsKey(invocationId)) {
      return evaluateInput(message, invocationId);
    }
    return evaluateOutput(message, invocationId);
  }

  private GuardrailResult evaluateInput(String message, String invocationId) {
    MaskingResult result = PiiDetector.detectAndMask(message);
    inputMappings.put(invocationId, result.hasPii() ? result.placeholderToOriginal() : Map.of());
    if (!result.hasPii()) {
      return GuardrailResult.allow();
    }
    return GuardrailResult.allowWithRewrite(result.maskedText());
  }

  private GuardrailResult evaluateOutput(String message, String invocationId) {
    Map<String, String> placeholderToOriginal = inputMappings.remove(invocationId);
    if (placeholderToOriginal.isEmpty()) {
      return GuardrailResult.allow();
    }
    String restored = PiiDetector.restore(message, placeholderToOriginal);
    return restored.equals(message) ? GuardrailResult.allow() : GuardrailResult.allowWithRewrite(restored);
  }
}
