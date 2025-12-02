package com.axonivy.utils.smart.workflow.guardrails.dummy;

import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail.GuardrailType;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

@SmartWorkflowGuardrail(type = GuardrailType.OUTPUT)
public class DummyOutputGuardrail implements OutputGuardrail {

  @Override
  public OutputGuardrailResult validate(AiMessage responseFromLLM) {
    return failure("Output guardrail founded !");
  }
}
