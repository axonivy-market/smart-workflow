package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class TestCircuitBreakerGuardrail {

  @Test
  void evaluate_signalSilent_allows() {
    var guardrail = new CircuitBreakerGuardrail(Optional::empty);

    var result = guardrail.evaluate("hello");

    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getReason()).isNull();
    assertThat(result.getCause()).isEmpty();
  }

  @Test
  void evaluate_signalTripped_blocksWithReasonAndTypedCause() {
    var guardrail = new CircuitBreakerGuardrail(() -> Optional.of("stopped for maintenance"));

    var result = guardrail.evaluate("hello");

    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).isEqualTo("stopped for maintenance");
    assertThat(result.getCause()).containsInstanceOf(CircuitBreakerOpenException.class);
    assertThat(result.getCause().get()).hasMessage("stopped for maintenance");
  }
}
