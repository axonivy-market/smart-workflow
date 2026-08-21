package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal.VariableCircuitBreakerSignal;

public interface CircuitBreakerSignal {

  Optional<String> stopReason();

  static CircuitBreakerSignal defaultSignal() {
    return new VariableCircuitBreakerSignal();
  }

  static void stopAll() {
    VariableCircuitBreakerSignal.setStopAll(true);
  }

  static void resumeAll() {
    VariableCircuitBreakerSignal.setStopAll(false);
  }
}
