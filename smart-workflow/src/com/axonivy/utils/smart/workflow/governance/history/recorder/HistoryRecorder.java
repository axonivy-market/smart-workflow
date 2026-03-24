package com.axonivy.utils.smart.workflow.governance.history.recorder;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public interface HistoryRecorder {

  record ResponseMetadata(Integer inputTokens, Integer outputTokens, Integer totalTokens,
      String finishReason, String modelName, Long durationMs,
      String aiServiceMethod, List<String> toolNames) {}

  void store(List<ChatMessage> messages, ResponseMetadata metadata);
}
