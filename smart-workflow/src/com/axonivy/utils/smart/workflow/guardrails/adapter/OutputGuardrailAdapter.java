package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class OutputGuardrailAdapter implements OutputGuardrail {

  private SmartWorkflowOutputGuardrail delegate;

  public OutputGuardrailAdapter(SmartWorkflowOutputGuardrail delegate) {
    this.delegate = delegate;
  }

  @Override
  public OutputGuardrailResult validate(AiMessage aiMessage) {
    String message = Optional.ofNullable(aiMessage).map(AiMessage::text).orElse(StringUtils.EMPTY);
    return doValidate(message);
  }

  @Override
  public OutputGuardrailResult validate(OutputGuardrailRequest request) {
    String message = Optional.ofNullable(request)
        .map(OutputGuardrailRequest::responseFromLLM)
        .map(response -> response.aiMessage())
        .map(AiMessage::text)
        .orElse(StringUtils.EMPTY);
    return doValidate(message);
  }

  private OutputGuardrailResult doValidate(String message) {
    GuardrailResult result = delegate.evaluate(message);

    if (!result.isAllowed()) {
      return this.fatal(result.getReason());
    }
    return this.success();
  }

  public SmartWorkflowOutputGuardrail getDelegate() {
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
    OutputGuardrailAdapter other = (OutputGuardrailAdapter) obj;
    if (delegate == null) {
      return other.delegate == null;
    }
    return delegate.getClass().getName().equals(
        other.delegate != null ? other.delegate.getClass().getName() : null);
  }
}
