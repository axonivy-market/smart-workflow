package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder.ResponseMetadata;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public class AgentResponseListener implements AiServiceListener<AiServiceResponseReceivedEvent> {

  private final HistoryRecorder recorder;

  public AgentResponseListener(HistoryRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public Class<AiServiceResponseReceivedEvent> getEventClass() {
    return AiServiceResponseReceivedEvent.class;
  }

  @Override
  public void onEvent(AiServiceResponseReceivedEvent event) {
    List<ChatMessage> all = new ArrayList<>(event.request().messages());
    all.add(event.response().aiMessage());
    recorder.store(all, buildMetadata(event));
  }

  private static ResponseMetadata buildMetadata(AiServiceResponseReceivedEvent event) {
    var context = event.invocationContext();
    var response = event.response();
    var usage = Optional.ofNullable(response.tokenUsage());
    long durationMs = System.currentTimeMillis() - context.timestamp().toEpochMilli();
    return new HistoryRecorder.ResponseMetadata(
        usage.map(TokenUsage::inputTokenCount).orElse(null),
        usage.map(TokenUsage::outputTokenCount).orElse(null),
        usage.map(TokenUsage::totalTokenCount).orElse(null),
        Optional.ofNullable(response.finishReason()).map(Enum::name).orElse(null),
        response.modelName(),
        durationMs,
        context.methodName(),
        toolNames(event));
  }

  private static List<String> toolNames(AiServiceResponseReceivedEvent event) {
    var aiMessage = event.response().aiMessage();
    var fromHistory = event.request().messages().stream()
        .filter(AiMessage.class::isInstance).map(AiMessage.class::cast)
        .flatMap(aiMsg -> Optional.ofNullable(aiMsg.toolExecutionRequests()).orElse(List.of()).stream());
    var fromResponse = Optional.ofNullable(aiMessage.toolExecutionRequests())
        .orElse(List.of())
        .stream();
    return Stream.concat(fromHistory, fromResponse)
        .map(ToolExecutionRequest::name)
        .toList();
  }
}
