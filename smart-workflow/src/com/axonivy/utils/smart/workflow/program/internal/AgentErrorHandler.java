package com.axonivy.utils.smart.workflow.program.internal;

import java.util.Optional;
import java.util.function.Supplier;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.guardrail.GuardrailException;

public class AgentErrorHandler {

  private static final String GUARDRAIL_ERROR = "Guardrail validation failed: {0}";

  public static <T> T executeWithErrorHandling(Supplier<T> execution, Optional<String> errorCode) {
    try {
      return execution.get();
    } catch (GuardrailException e) {
      Ivy.log().error(GUARDRAIL_ERROR, e.getMessage());
      errorCode.ifPresent(code -> {
        BpmError.create(code).withMessage(e.getMessage()).withCause(e.getCause()).throwError();
      });
      return null;
    }
  }
}
