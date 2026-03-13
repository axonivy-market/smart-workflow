package com.axonivy.utils.smart.workflow.governance.history;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public interface HistoryRecorder {

  record ResponseMetadata(Integer inputTokens, Integer outputTokens, Integer totalTokens,
      String finishReason, String modelName, Long durationMs) {}

  void store(List<ChatMessage> messages, ResponseMetadata metadata);
}
