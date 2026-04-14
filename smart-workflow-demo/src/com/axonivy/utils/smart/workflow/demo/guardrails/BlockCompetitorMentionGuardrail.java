package com.axonivy.utils.smart.workflow.demo.guardrails;

import java.util.Locale;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

/**
 * A custom input guardrail that blocks messages mentioning competitor products.
 * Demonstrates how to implement {@link SmartWorkflowInputGuardrail} via SPI.
 */
public class BlockCompetitorMentionGuardrail implements SmartWorkflowInputGuardrail {

  private static final String[] COMPETITORS = {
      "flowsphere", "taskpilot", "processx", "workbridge", "autoflow", "nexiflow", "orbitbpm"
  };

  @Override
  public GuardrailResult evaluate(String message) {
    if (message == null) {
      return GuardrailResult.allow();
    }
    String lower = message.toLowerCase(Locale.ROOT);
    for (String competitor : COMPETITORS) {
      if (lower.contains(competitor)) {
        return GuardrailResult.block(
            "Message blocked: competitor product '" + competitor + "' mentioned. "
            + "Do not reference competitor products in AI queries.");
      }
    }
    return GuardrailResult.allow();
  }

  @Override
  public String name() {
    return "BlockCompetitorMentionGuardrail";
  }
}
