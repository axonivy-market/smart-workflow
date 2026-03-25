package com.axonivy.utils.smart.workflow.observability.openinference;


import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.observability.openinference.internal.OpenInferenceCollector;
import com.axonivy.utils.smart.workflow.observability.openinference.span.LLMSpan;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

public class OpenInferenceTracing implements ChatModelListener {

  public interface Var {
    String PREFIX = "AI.Observability.Openinference.";
    String ENABLED = PREFIX + "Enabled";
    String HIDE_INPUT_MESSAGES = PREFIX + "HideInputMessages";
    String HIDE_OUTPUT_MESSAGES = PREFIX + "HideOutputMessages";
  }

  private final OpenInferenceCollector collector;
  private Span<Void> span;

  public OpenInferenceTracing(String provider) {
    this.collector = new OpenInferenceCollector(provider)
        .hideInputMessages(IvyVar.bool(Var.HIDE_INPUT_MESSAGES))
        .hideOutputMessages(IvyVar.bool(Var.HIDE_OUTPUT_MESSAGES)) ;
  }

  private List<Attribute> attributes() {
    return collector.getAttributes().entrySet().stream()
        .map(e -> Attribute.attribute(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public void onRequest(ChatModelRequestContext requestContext) {
    this.collector.onRequest(requestContext);
    this.span = Span.open(() -> new LLMSpan(this::attributes));
  }

  @Override
  public void onResponse(ChatModelResponseContext responseContext) {
    this.collector.onResponse(responseContext);
    this.span.result(null);
    this.span.close();
  }

  @Override
  public void onError(ChatModelErrorContext errorContext) {
    if (span == null) {
      Ivy.log().error("Error occurred before span was created", errorContext.error());
      return;
    }
    this.span.error(errorContext.error());
    this.span.close();
  }
}
