package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.Optional;

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
    return doValidate(GuardrailInspectionText.text(userMessage));
  }

  @Override
  public InputGuardrailResult validate(InputGuardrailRequest request) {
    String message = GuardrailInspectionText.text(
        Optional.ofNullable(request).map(InputGuardrailRequest::userMessage).orElse(null));
    String invocationId = Optional.ofNullable(request)
        .map(InputGuardrailRequest::requestParams)
        .map(p -> p.invocationContext())
        .map(ctx -> ctx.invocationId().toString())
        .orElse(null);
    return doValidate(message, invocationId);
  }

  private InputGuardrailResult doValidate(String message) {
    return doValidate(message, null);
  }

  private InputGuardrailResult doValidate(String message, String invocationId) {
    GuardrailResult result = invocationId != null
        ? getDelegate().evaluate(message, invocationId)
        : getDelegate().evaluate(message);
    if (!result.isAllowed()) {
      return failure(result.getReason());
    }
    return result.getRewrittenMessage().map(this::successWith).orElseGet(this::success);
  }
}
