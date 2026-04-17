package com.axonivy.utils.smart.workflow.observability.openinference;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.arize.semconv.trace.SemanticConventions;
import com.axonivy.utils.smart.workflow.observability.openinference.internal.GuardrailRecorder;
import com.axonivy.utils.smart.workflow.observability.openinference.internal.OpenInferenceCollector;
import com.axonivy.utils.smart.workflow.observability.openinference.internal.ToolCollector;
import com.axonivy.utils.smart.workflow.observability.openinference.span.AiSpan;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.GuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceRequestIssuedListener;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.observability.api.listener.InputGuardrailExecutedListener;
import dev.langchain4j.observability.api.listener.OutputGuardrailExecutedListener;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;

public class OpenInferenceTracing implements ChatModelListener {

  public interface Var {
    String PREFIX = "AI.Observability.Openinference.";
    String ENABLED = PREFIX + "Enabled";
    String HIDE_INPUT_MESSAGES = PREFIX + "HideInputMessages";
    String HIDE_OUTPUT_MESSAGES = PREFIX + "HideOutputMessages";
  }

  private final MessageOptions options;
  private final OpenInferenceCollector collector;
  private Span<Void> llmSpan;
  private Span<Void> agentSpan;

  private final Map<String, ToolExecution> toolExecutions = new ConcurrentHashMap<>();
  private record ToolExecution(Span<Void> span, ToolCollector collector) {}

  public OpenInferenceTracing(String provider, String model) {
    this.options = MessageOptions.fromIvyVar();
    this.collector = new OpenInferenceCollector(provider, model)
        .hideInputMessages(options.hideInput())
        .hideOutputMessages(options.hideOutput()) ;
  }

  public record MessageOptions(boolean hideInput, boolean hideOutput) {
    public static MessageOptions fromIvyVar() {
      return new MessageOptions(
          IvyVar.bool(Var.HIDE_INPUT_MESSAGES),
          IvyVar.bool(Var.HIDE_OUTPUT_MESSAGES));
    }
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
      new InitListener(),
      new CompletedListener(),
      new RequestListener(),
      new ResponseListener(),
      new ErrorListener(),
      new ToolListener(),
      new InputGuardrailTracingListener(),
      new OutputGuardrailTracingListener());
  }

  private class InitListener implements AiServiceStartedListener {

    @Override
    public void onEvent(AiServiceStartedEvent event) {
      agentSpan = Span.open().instance(() -> new AiSpan("AI Agent", () -> List.of(
          Attribute.attribute(SemanticConventions.OPENINFERENCE_SPAN_KIND,
              SemanticConventions.OpenInferenceSpanKind.AGENT.getValue()))));
    }
  }

  private class CompletedListener implements AiServiceCompletedListener {
    
    @Override
    public void onEvent(AiServiceCompletedEvent event) {
      agentSpan.result(null);
      agentSpan.close();
    }
  }

  private class RequestListener implements AiServiceRequestIssuedListener {

    @Override
    public void onEvent(AiServiceRequestIssuedEvent event) {
      collector.onRequest(event.request());
      llmSpan = Span.open().instance(() -> new AiSpan("AI Assistant", () -> attributes()));
    }
  }

  private class ResponseListener implements AiServiceResponseReceivedListener {

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
      collector.onResponse(event.response());
      llmSpan.result(null);
      llmSpan.close();

      Optional.ofNullable(event.response().aiMessage()).stream()
        .filter(AiMessage::hasToolExecutionRequests)
        .flatMap(msg -> msg.toolExecutionRequests().stream()).forEachOrdered(request -> {
          ToolCollector toolCollector = new ToolCollector(options);
          toolCollector.onRequestExecution(request);
          Span<Void> toolSpan = Span.open().instance(
            () -> new AiSpan("Tool", () -> toolCollector.getAttributes()));
          toolExecutions.put(request.id(), new ToolExecution(toolSpan, toolCollector));
      });
    }
  }

  private class ErrorListener implements AiServiceErrorListener {

    @Override
    public void onEvent(AiServiceErrorEvent event) {
      toolExecutions.forEach((id, execution) -> {
        if (toolExecutions.remove(id, execution)) {
          execution.span().error(event.error());
          execution.span().close();
        }
      });
      if (llmSpan != null) {
        llmSpan.error(event.error());
        llmSpan.close();
      }
      if (agentSpan != null) {
        agentSpan.error(event.error());
        agentSpan.close();
      }
    }
  }

  private class ToolListener implements ToolExecutedEventListener {
    @Override
    public void onEvent(ToolExecutedEvent event) {
      var execution = toolExecutions.remove(event.request().id());
      if (execution == null) {
        return;
      }
      execution.collector().onExecuted(event);
      execution.span().result(null);
      execution.span().close();
    }
  }

  private class InputGuardrailTracingListener implements InputGuardrailExecutedListener {

    @Override
    public void onEvent(InputGuardrailExecutedEvent event) {
      String inputMessage = options.hideInput ? null : Optional.ofNullable(event.request())
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
      String outputMessage = options.hideOutput ? null : Optional.ofNullable(event.request())
          .map(r -> r.responseFromLLM())
          .map(r -> r.aiMessage())
          .map(AiMessage::text)
          .orElse(null);
      String guardrailName = event.guardrailClass().getSimpleName();
      traceGuardrail(event, "OUTPUT", outputMessage, guardrailName);
    }
  }

  private void traceGuardrail(GuardrailExecutedEvent<?, ?, ?> event, String type,
      String validatedMessage, String guardrailName) {
    Map<String, Object> attrs = new GuardrailRecorder()
        .handleGuardrail(event, type, validatedMessage, guardrailName);
    List<Attribute> attrList = attrs.entrySet().stream()
        .map(e -> Attribute.attribute(e.getKey(), e.getValue()))
        .toList();
    var guardrailSpan = Span.open().instance(() -> new AiSpan(guardrailName, () -> attrList));
    guardrailSpan.result(null);
    guardrailSpan.close();
  }
}
