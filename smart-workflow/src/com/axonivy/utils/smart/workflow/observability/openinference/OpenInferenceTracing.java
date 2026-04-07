package com.axonivy.utils.smart.workflow.observability.openinference;


import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.observability.openinference.internal.OpenInferenceCollector;
import com.axonivy.utils.smart.workflow.observability.openinference.span.LLMSpan;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceRequestIssuedListener;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;

public class OpenInferenceTracing implements ChatModelListener {

  public interface Var {
    String PREFIX = "AI.Observability.Openinference.";
    String ENABLED = PREFIX + "Enabled";
    String HIDE_INPUT_MESSAGES = PREFIX + "HideInputMessages";
    String HIDE_OUTPUT_MESSAGES = PREFIX + "HideOutputMessages";
  }

  private final OpenInferenceCollector collector;
  private Span<Void> span;

  public OpenInferenceTracing(String provider, String model) {
    this.collector = new OpenInferenceCollector(provider, model)
        .hideInputMessages(IvyVar.bool(Var.HIDE_INPUT_MESSAGES))
        .hideOutputMessages(IvyVar.bool(Var.HIDE_OUTPUT_MESSAGES)) ;
  }

  private List<Attribute> attributes() {
    return collector.getAttributes().entrySet().stream()
        .map(e -> Attribute.attribute(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  public List<AiServiceListener<?>> configure() {
    if (!IvyVar.bool(OpenInferenceTracing.Var.ENABLED)) {
      return List.of();
    }
    return List.of(
      new RequestListener(), 
      new ResponseListener(), 
      new ErrorListener());
  }

  private class RequestListener implements AiServiceRequestIssuedListener {

    @Override
    public void onEvent(AiServiceRequestIssuedEvent event) {
      collector.onRequest(event.request());
      span = Span.open(() -> new LLMSpan(() -> attributes()));
    }
  }

  private class ResponseListener implements AiServiceResponseReceivedListener {

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
      collector.onResponse(event.response());
      span.result(null);
      span.close();
    }
  }

  private class ErrorListener implements AiServiceErrorListener {

    @Override
    public void onEvent(AiServiceErrorEvent event) {
      if (span == null) {
        Ivy.log().error("Error occurred before span was created", event.error());
        return;
      }
      span.error(event.error());
      span.close();
    }
  }

}
