package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

/**
 * Marks a guardrail block as caused by an open circuit breaker rather than a content violation.
 *
 * <p>Attached as the cause of {@link com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult#block(String, Throwable)}
 * so it travels through langchain4j's guardrail exception and lets
 * {@link com.axonivy.utils.smart.workflow.guardrails.GuardrailErrors} raise a dedicated BPM error code.
 */
public class CircuitBreakerOpenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CircuitBreakerOpenException(String reason) {
    super(reason);
  }
}
