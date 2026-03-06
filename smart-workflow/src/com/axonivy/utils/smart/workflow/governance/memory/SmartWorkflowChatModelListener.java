package com.axonivy.utils.smart.workflow.governance.memory;

import java.util.Optional;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

public class SmartWorkflowChatModelListener implements ChatModelListener {

  public static class ResponseMetadata {
    public Integer inputTokens;
    public Integer outputTokens;
    public Integer totalTokens;
    public String finishReason;
    public String modelName;
    public Long durationMs;

    /** No-arg constructor required for Jackson deserialisation. */
    public ResponseMetadata() {}

    public ResponseMetadata(Integer inputTokens, Integer outputTokens, Integer totalTokens,
        String finishReason, String modelName, Long durationMs) {
      this.inputTokens = inputTokens;
      this.outputTokens = outputTokens;
      this.totalTokens = totalTokens;
      this.finishReason = finishReason;
      this.modelName = modelName;
      this.durationMs = durationMs;
    }
  }

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
    Optional<TokenUsage> usage = Optional.ofNullable(response.tokenUsage());
    Optional<ChatResponse> responseOpt = Optional.ofNullable(response);

    pending = new ResponseMetadata(
        usage.map(TokenUsage::inputTokenCount).orElse(null),
        usage.map(TokenUsage::outputTokenCount).orElse(null),
        usage.map(TokenUsage::totalTokenCount).orElse(null),
        responseOpt.map(ChatResponse::finishReason).map(Enum::name).orElse(null),
        responseOpt.map(ChatResponse::modelName).orElse(null),
        durationMs);
  }

  public ResponseMetadata drainPending() {
    var metadata = pending;
    pending = null;
    return metadata;
  }
}
