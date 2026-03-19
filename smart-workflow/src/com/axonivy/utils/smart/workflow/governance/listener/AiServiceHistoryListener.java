package com.axonivy.utils.smart.workflow.governance.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.history.HistoryRecorder;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public class AiServiceHistoryListener implements AiServiceListener<AiServiceResponseReceivedEvent> {

  private final String caseUuid;
  private final String taskUuid;
  private HistoryRecorder recorder;

  public AiServiceHistoryListener(String caseUuid, String taskUuid) {
    this.caseUuid = caseUuid;
    this.taskUuid = taskUuid;
  }

  public AiServiceHistoryListener(HistoryRecorder recorder) {
    this.caseUuid = null;
    this.taskUuid = null;
    this.recorder = recorder;
  }

  @Override
  public Class<AiServiceResponseReceivedEvent> getEventClass() {
    return AiServiceResponseReceivedEvent.class;
  }

  @Override
  public void onEvent(AiServiceResponseReceivedEvent event) {
    var ctx = event.invocationContext();
    if (recorder == null) {
      recorder = new ChatHistoryRepository(caseUuid, taskUuid, ctx.invocationId().toString());
    }

    long durationMs = System.currentTimeMillis() - ctx.timestamp().toEpochMilli();
    var response = event.response();
    var usage = Optional.ofNullable(response.tokenUsage());
    var metadata = new HistoryRecorder.ResponseMetadata(
        usage.map(TokenUsage::inputTokenCount).orElse(null),
        usage.map(TokenUsage::outputTokenCount).orElse(null),
        usage.map(TokenUsage::totalTokenCount).orElse(null),
        Optional.ofNullable(response.finishReason()).map(Enum::name).orElse(null),
        response.modelName(),
        durationMs,
        ctx.methodName(),
        toolNames(event));

    List<ChatMessage> all = new ArrayList<>(event.request().messages());
    all.add(response.aiMessage());
    recorder.store(all, metadata);
  }

  private static List<String> toolNames(AiServiceResponseReceivedEvent event) {
    List<String> names = new ArrayList<>();
    event.request().messages().stream()
        .filter(AiMessage.class::isInstance).map(AiMessage.class::cast)
        .filter(AiMessage::hasToolExecutionRequests)
        .flatMap(m -> m.toolExecutionRequests().stream())
        .map(ToolExecutionRequest::name)
        .forEach(names::add);
    if (event.response().aiMessage().hasToolExecutionRequests()) {
      event.response().aiMessage().toolExecutionRequests().stream()
          .map(ToolExecutionRequest::name)
          .forEach(names::add);
    }
    return List.copyOf(names);
  }
}
