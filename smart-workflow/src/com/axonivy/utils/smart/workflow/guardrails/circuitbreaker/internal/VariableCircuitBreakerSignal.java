package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.CircuitBreakerSignal;

import ch.ivyteam.ivy.environment.Ivy;

public class VariableCircuitBreakerSignal implements CircuitBreakerSignal {

  static final String ENABLED_VARIABLE = "AI.CircuitBreaker.Enabled";

  private static final String STOP_MESSAGE = "All AI agent activities are currently stopped.";

  @Override
  public Optional<String> stopReason() {
    try {
      if (!"true".equalsIgnoreCase(Ivy.var().get(ENABLED_VARIABLE))) {
        return Optional.empty();
      }
      return Optional.of(STOP_MESSAGE);
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  public static void setStopAll(boolean stop) {
    Ivy.var().set(ENABLED_VARIABLE, Boolean.toString(stop));
  }
}
