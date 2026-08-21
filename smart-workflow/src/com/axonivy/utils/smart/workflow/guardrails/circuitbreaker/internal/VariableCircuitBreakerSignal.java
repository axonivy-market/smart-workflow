package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.CircuitBreakerSignal;

import ch.ivyteam.ivy.environment.Ivy;

public class VariableCircuitBreakerSignal implements CircuitBreakerSignal {

  static final String STOP_ALL_VARIABLE = "AI.CircuitBreaker.StopAll";

  private static final String STOP_MESSAGE = "All AI agent activities are currently stopped.";

  @Override
  public Optional<String> stopReason() {
    try {
      if (!"true".equalsIgnoreCase(Ivy.var().get(STOP_ALL_VARIABLE))) {
        return Optional.empty();
      }
      return Optional.of(STOP_MESSAGE);
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  public static void setStopAll(boolean stop) {
    Ivy.var().set(STOP_ALL_VARIABLE, Boolean.toString(stop));
  }
}
