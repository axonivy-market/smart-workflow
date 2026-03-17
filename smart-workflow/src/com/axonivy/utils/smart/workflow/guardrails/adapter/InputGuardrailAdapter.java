package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;

public class InputGuardrailAdapter extends AbstractGuardrailAdapter<SmartWorkflowInputGuardrail> implements InputGuardrail {

  public InputGuardrailAdapter(SmartWorkflowInputGuardrail delegate) {
    super(delegate);
  }

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
    GuardrailResult result = getDelegate().evaluate(message);
    return result.isAllowed() ? success() : failure(result.getReason());
  }
}
