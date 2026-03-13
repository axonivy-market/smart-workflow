package com.axonivy.utils.smart.workflow.governance.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.history.HistoryRecorder;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

public class ChatHistoryRecordingListener implements ChatModelListener {

  private final HistoryRecorder recorder;
  private long requestStartMs;

  public ChatHistoryRecordingListener(String caseUuid, String taskUuid) {
    this.recorder = new ChatHistoryRepository(caseUuid, taskUuid);
  }

  public ChatHistoryRecordingListener(HistoryRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  public void onRequest(ChatModelRequestContext ctx) {
    requestStartMs = System.currentTimeMillis();
  }

  @Override
  public void onResponse(ChatModelResponseContext ctx) {
    long durationMs = System.currentTimeMillis() - requestStartMs;
    var response = ctx.chatResponse();
    var usage = Optional.ofNullable(response.tokenUsage());
    var metadata = new HistoryRecorder.ResponseMetadata(
        usage.map(TokenUsage::inputTokenCount).orElse(null),
        usage.map(TokenUsage::outputTokenCount).orElse(null),
        usage.map(TokenUsage::totalTokenCount).orElse(null),
        Optional.ofNullable(response.finishReason()).map(Enum::name).orElse(null),
        response.modelName(),
        durationMs);

    List<ChatMessage> all = new ArrayList<>(ctx.chatRequest().messages());
    all.add(response.aiMessage());
    recorder.store(all, metadata);
  }
}
