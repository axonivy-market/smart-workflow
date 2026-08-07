package com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.internal;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;

import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;
import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.CircuitBreakerSignal;

import ch.ivyteam.ivy.environment.Ivy;

public class VariableCircuitBreakerSignal implements CircuitBreakerSignal {


  private static final String DEFAULT_MESSAGE = "All AI agent activities are currently stopped.";

  @Override
  public Optional<String> stopReason() {
    try {
      if (!BooleanUtils.toBoolean(Ivy.var().get(CircuitBreakerSignal.STOP_ALL_VARIABLE))) {
        return Optional.empty();
      }
      return Optional.of(buildReason());
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  private static String buildReason() {
    try {
      String user = Ivy.session().getSessionUserName();
      String time = LocalDateTime.now().format(DatePatternUtils.dateTimeFormatter());
      return "All AI agent activities are stopped by " + user + " at " + time;
    } catch (RuntimeException ex) {
      return DEFAULT_MESSAGE;
    }
  }
}
