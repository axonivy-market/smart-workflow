package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestVariableCircuitBreakerSignal {

  private final VariableCircuitBreakerSignal signal = new VariableCircuitBreakerSignal();

  @Test
  void stopReason_variableTrue_isTripped(AppFixture fixture) {
    fixture.var(VariableCircuitBreakerSignal.STOP_ALL_VARIABLE, "true");

    assertThat(signal.stopReason()).contains("All AI agent activities are currently stopped.");
  }

  @Test
  void stopReason_variableTrueIgnoresCase_isTripped(AppFixture fixture) {
    fixture.var(VariableCircuitBreakerSignal.STOP_ALL_VARIABLE, "TrUe");

    assertThat(signal.stopReason()).isNotEmpty();
  }

  /**
   * The breaker deliberately accepts only "true": a typo in the variable must fail safe by keeping agents
   * running rather than silently stopping the whole application.
   */
  @Test
  void stopReason_looseBooleanValues_isNotTripped(AppFixture fixture) {
    for (String value : List.of("false", "", " ", "yes", "on", "y", "t", "1", "true ")) {
      fixture.var(VariableCircuitBreakerSignal.STOP_ALL_VARIABLE, value);

      assertThat(signal.stopReason()).as("value '%s'", value).isEmpty();
    }
  }
}
