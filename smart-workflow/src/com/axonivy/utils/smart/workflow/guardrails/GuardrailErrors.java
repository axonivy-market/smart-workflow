package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Optional;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import dev.langchain4j.guardrail.InputGuardrailException;

public final class GuardrailErrors {
  private static final String INPUT_VIOLATION = "smartworkflow:guardrail:input:violation";
  private static final String OUTPUT_VIOLATION = "smartworkflow:guardrail:output:violation";

  private GuardrailErrors() {}

  public static void throwError(Exception ex) {
    String errorCode = ex instanceof InputGuardrailException ? INPUT_VIOLATION : OUTPUT_VIOLATION;
    BpmPublicErrorBuilder errorBuilder = BpmError.create(errorCode);
    Optional.ofNullable(ex.getMessage()).ifPresent(errorBuilder::withMessage);
    errorBuilder.withCause(ex);
    errorBuilder.throwError();
  }
}
