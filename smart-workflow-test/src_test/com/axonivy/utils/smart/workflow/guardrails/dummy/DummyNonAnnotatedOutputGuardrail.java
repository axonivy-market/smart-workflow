package com.axonivy.utils.smart.workflow.guardrails.dummy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class DummyNonAnnotatedOutputGuardrail implements OutputGuardrail {

  @Override
  public OutputGuardrailResult validate(AiMessage responseFromLLM) {
    return failure("Non-annotated output guardrail founded !");
  }

}
