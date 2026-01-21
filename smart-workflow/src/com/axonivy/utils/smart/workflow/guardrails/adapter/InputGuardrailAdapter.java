package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;

public class InputGuardrailAdapter implements InputGuardrail {

  private SmartWorkflowInputGuardrail delegate;

  public InputGuardrailAdapter(SmartWorkflowInputGuardrail delegate) {
    this.delegate = delegate;
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
    GuardrailResult result = delegate.evaluate(message);

    if (!result.isAllowed()) {
      return this.failure(result.getReason());
    }
    return this.success();
  }

  public SmartWorkflowInputGuardrail getDelegate() {
    return delegate;
  }

  @Override
  public int hashCode() {
    return delegate != null ? delegate.getClass().getName().hashCode() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    InputGuardrailAdapter other = (InputGuardrailAdapter) obj;
    if (delegate == null) {
      return other.delegate == null;
    }
    return delegate.getClass().getName().equals(
        other.delegate != null ? other.delegate.getClass().getName() : null);
  }
}
