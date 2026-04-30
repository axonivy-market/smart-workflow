package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class OutputGuardrailAdapter extends AbstractGuardrailAdapter<SmartWorkflowOutputGuardrail> implements OutputGuardrail {

  public OutputGuardrailAdapter(SmartWorkflowOutputGuardrail delegate) {
    super(delegate);
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
    String invocationId = Optional.ofNullable(request)
        .map(OutputGuardrailRequest::requestParams)
        .map(p -> p.invocationContext())
        .map(ctx -> ctx.invocationId().toString())
        .orElse(null);
    return doValidate(message, invocationId);
  }

  private OutputGuardrailResult doValidate(String message) {
    return doValidate(message, null);
  }

  private OutputGuardrailResult doValidate(String message, String invocationId) {
    GuardrailResult result = invocationId != null
        ? getDelegate().evaluate(message, invocationId)
        : getDelegate().evaluate(message);
    if (!result.isAllowed()) {
      return fatal(result.getReason());
    }
    return result.hasRewrite() ? successWith(result.getRewrittenMessage()) : success();
  }
}
