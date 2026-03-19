package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Optional;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;

public final class GuardrailErrors {
  public static final String INPUT_VIOLATION = "smartworkflow:guardrail:input:violation";
  public static final String OUTPUT_VIOLATION = "smartworkflow:guardrail:output:violation";

  private GuardrailErrors() {}

  public static void throwError(String errorCode, Exception ex) {
    BpmPublicErrorBuilder errorBuilder = BpmError.create(errorCode);
    Optional.ofNullable(ex.getMessage()).ifPresent(errorBuilder::withMessage);
    Optional.ofNullable(ex.getCause()).ifPresent(errorBuilder::withCause);
    errorBuilder.throwError();
  }
}
