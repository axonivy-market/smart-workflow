package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

public class CircuitBreakerGuardrail implements SmartWorkflowInputGuardrail, SmartWorkflowOutputGuardrail {

  private final CircuitBreakerSignal signal;

  public CircuitBreakerGuardrail() {
    this(CircuitBreakerSignal.defaultSignal());
  }

  public CircuitBreakerGuardrail(CircuitBreakerSignal signal) {
    this.signal = signal;
  }

  @Override
  public boolean alwaysOn() {
    return true;
  }

  @Override
  public GuardrailResult evaluate(String message) {
    return signal.stopReason()
        .map(reason -> GuardrailResult.block(reason, new CircuitBreakerOpenException(reason)))
        .orElseGet(GuardrailResult::allow);
  }
}
