package com.axonivy.utils.smart.workflow.governance.memory;

import java.util.Optional;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

public class SmartWorkflowChatModelListener implements ChatModelListener {

  public record ResponseMetadata(Integer inputTokens, Integer outputTokens, Integer totalTokens,
      String finishReason, String modelName, Long durationMs) {}

  private ResponseMetadata pending;
  private long requestStartMs;

  @Override
  public void onRequest(ChatModelRequestContext ctx) {
    requestStartMs = System.currentTimeMillis();
  }

  @Override
  public void onResponse(ChatModelResponseContext ctx) {
    long durationMs = System.currentTimeMillis() - requestStartMs;
    var response = ctx.chatResponse();
    var usage = Optional.ofNullable(response.tokenUsage());

    pending = new ResponseMetadata(
        usage.map(TokenUsage::inputTokenCount).orElse(null),
        usage.map(TokenUsage::outputTokenCount).orElse(null),
        usage.map(TokenUsage::totalTokenCount).orElse(null),
        response.finishReason() != null ? response.finishReason().name() : null,
        response.modelName(),
        durationMs);
  }

  public ResponseMetadata drainPending() {
    var metadata = pending;
    pending = null;
    return metadata;
  }
}
