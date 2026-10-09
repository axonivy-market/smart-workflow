package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.internal.SmartWorkflowInternalInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.internal.SmartWorkflowInternalOutputGuardrail;

public class CircuitBreakerGuardrail implements SmartWorkflowInternalInputGuardrail, SmartWorkflowInternalOutputGuardrail {

  private final CircuitBreakerSignal signal;

  public CircuitBreakerGuardrail() {
    this(CircuitBreakerSignal.defaultSignal());
  }

  public CircuitBreakerGuardrail(CircuitBreakerSignal signal) {
    this.signal = signal;
  }

  @Override
  public GuardrailResult evaluate(String message) {
    return signal.stopReason()
        .map(reason -> GuardrailResult.block(reason, new CircuitBreakerOpenException(reason)))
        .orElseGet(GuardrailResult::allow);
  }
}
