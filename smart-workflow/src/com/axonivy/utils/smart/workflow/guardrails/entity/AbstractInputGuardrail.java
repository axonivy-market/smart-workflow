package com.axonivy.utils.smart.workflow.guardrails.entity;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;

public abstract class AbstractInputGuardrail implements SmartWorkflowInputGuardrail, InputGuardrail {
  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String message = Optional.ofNullable(userMessage).map(UserMessage::singleText).orElse(StringUtils.EMPTY);
    return doValidate(message);
  }

  @Override
  public InputGuardrailResult validate(InputGuardrailRequest request) {
    String message = Optional.ofNullable(request).map(InputGuardrailRequest::userMessage).map(UserMessage::singleText)
        .orElse(StringUtils.EMPTY);
    return doValidate(message);
  }
  
  private InputGuardrailResult doValidate(String message) {
    GuardrailResult result = evaluate(message);

    if (!result.isAllowed()) {
      failure(result.getReason());
      return this.failure(result.getReason());
    }
    return this.success();
  }

  @Override
  public abstract String name();

  @Override
  public abstract GuardrailResult evaluate(String message);
}
