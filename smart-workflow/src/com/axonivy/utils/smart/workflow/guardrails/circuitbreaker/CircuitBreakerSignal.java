package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal.VariableCircuitBreakerSignal;

public interface CircuitBreakerSignal {

  String STOP_ALL_VARIABLE = "AI.CircuitBreaker.StopAll";

  Optional<String> stopReason();

  static CircuitBreakerSignal defaultSignal() {
    return new VariableCircuitBreakerSignal();
  }
}
