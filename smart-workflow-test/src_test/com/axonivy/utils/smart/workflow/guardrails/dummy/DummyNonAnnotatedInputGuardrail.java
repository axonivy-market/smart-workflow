package com.axonivy.utils.smart.workflow.guardrails.dummy;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

public class DummyNonAnnotatedInputGuardrail implements InputGuardrail {

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    return failure("Non-annotated input guardrail founded!");
  }
}