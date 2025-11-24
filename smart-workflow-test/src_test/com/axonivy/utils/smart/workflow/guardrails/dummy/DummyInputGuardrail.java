package com.axonivy.utils.smart.workflow.guardrails.dummy;

import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail.GuardrailType;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

@SmartWorkflowGuardrail(type = GuardrailType.INPUT)
public class DummyInputGuardrail implements InputGuardrail {

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    return failure("Input guardrail founded!");
  }
}
