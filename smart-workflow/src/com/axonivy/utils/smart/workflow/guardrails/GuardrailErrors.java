package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.CircuitBreakerOpenException;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import dev.langchain4j.guardrail.InputGuardrailException;

public final class GuardrailErrors {
  private static final String INPUT_VIOLATION = "smartworkflow:guardrail:input:violation";
  private static final String OUTPUT_VIOLATION = "smartworkflow:guardrail:output:violation";
  private static final String STOP = "smartworkflow:stop";

  private GuardrailErrors() {}

  public static void throwError(Exception ex) {
    BpmPublicErrorBuilder errorBuilder = BpmError.create(errorCode(ex));
    Optional.ofNullable(ex.getMessage()).ifPresent(errorBuilder::withMessage);
    errorBuilder.withCause(ex);
    errorBuilder.throwError();
  }

  private static String errorCode(Exception ex) {
    if (ExceptionUtils.throwableOfType(ex, CircuitBreakerOpenException.class) != null) {
      return STOP;
    }
    return ex instanceof InputGuardrailException ? INPUT_VIOLATION : OUTPUT_VIOLATION;
  }
}
