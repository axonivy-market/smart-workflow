package com.axonivy.utils.smart.workflow.observability.openinference;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.arize.semconv.trace.SemanticConventions;
import com.axonivy.utils.smart.workflow.observability.openinference.internal.OpenInferenceCollector;
import com.axonivy.utils.smart.workflow.observability.openinference.span.GuardrailSpan;
import com.axonivy.utils.smart.workflow.observability.openinference.span.LLMSpan;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailResult.Failure;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.GuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceRequestIssuedListener;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.observability.api.listener.InputGuardrailExecutedListener;
import dev.langchain4j.observability.api.listener.OutputGuardrailExecutedListener;

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
      new ErrorListener(),
      new InputGuardrailTracingListener(),
      new OutputGuardrailTracingListener());
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

  private class InputGuardrailTracingListener implements InputGuardrailExecutedListener {

    @Override
    public void onEvent(InputGuardrailExecutedEvent event) {
      String inputMessage = Optional.ofNullable(event.request())
          .map(r -> r.userMessage())
          .map(UserMessage::singleText)
          .orElse(null);
      String guardrailName = event.guardrailClass().getSimpleName();
      traceGuardrail(event, "INPUT", inputMessage, guardrailName);
    }
  }

  private class OutputGuardrailTracingListener implements OutputGuardrailExecutedListener {

    @Override
    public void onEvent(OutputGuardrailExecutedEvent event) {
      String outputMessage = Optional.ofNullable(event.request())
          .map(r -> r.responseFromLLM())
          .map(r -> r.aiMessage())
          .map(AiMessage::text)
          .orElse(null);
      String guardrailName = event.guardrailClass().getSimpleName();
      traceGuardrail(event, "OUTPUT", outputMessage, guardrailName);
    }
  }

  private void traceGuardrail(GuardrailExecutedEvent<?, ?, ?> event, String type, String validatedMessage, String guardrailName) {
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put(SemanticConventions.OPENINFERENCE_SPAN_KIND,
        SemanticConventions.OpenInferenceSpanKind.GUARDRAIL.getValue());

    var result = event.result();
    boolean passed = result.failures().isEmpty();

    attrs.put("validator_name", guardrailName);
    // Smart Workflow adapters always throw on block (InputGuardrailAdapter uses failure(),
    // OutputGuardrailAdapter uses fatal()), so on_fail is always "exception".
    // If we support retry/reprompt in the future, derive from OutputGuardrailResult.isRetry()/isReprompt().
    attrs.put("validator_on_fail", "exception");

    attrs.put("guardrail.type", type);
    attrs.put("guardrail.result", result.result().name());

    if (validatedMessage != null) {
      attrs.put(SemanticConventions.INPUT_VALUE, validatedMessage);
      attrs.put(SemanticConventions.INPUT_MIME_TYPE, "text/plain");
    }

    if (passed) {
      attrs.put(SemanticConventions.OUTPUT_VALUE, "pass");
    } else {
      attrs.put(SemanticConventions.OUTPUT_VALUE, "fail");
      List<String> messages = new ArrayList<>();
      for (var failure : result.failures()) {
        if (failure instanceof Failure f && f.message() != null) {
          messages.add(f.message());
        }
      }
      if (!messages.isEmpty()) {
        attrs.put("guardrail.failure_message", String.join("; ", messages));
      }
    }
    attrs.put(SemanticConventions.OUTPUT_MIME_TYPE, "text/plain");

    List<Attribute> attrList = attrs.entrySet().stream()
        .map(e -> Attribute.attribute(e.getKey(), e.getValue()))
        .collect(Collectors.toList());

    var guardrailSpan = Span.open(() -> new GuardrailSpan(guardrailName, () -> attrList));
    guardrailSpan.result(null);
    guardrailSpan.close();
  }

}
