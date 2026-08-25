package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

public class CircuitBreakerOpenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CircuitBreakerOpenException(String reason) {
    super(reason);
  }
}
