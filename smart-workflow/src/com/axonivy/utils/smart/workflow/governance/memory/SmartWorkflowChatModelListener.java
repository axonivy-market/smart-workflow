package com.axonivy.utils.smart.workflow.governance.memory;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

/**
 * Captures token usage, finish reason and model name from each LLM response.
 * One instance is created per agent execution and shared between
 * {@link IvyChatMemoryStore} and the {@code ListeningChatModel} wrapper.
 *
 * <p>The listener fires synchronously on the same thread as the ensuing
 * {@code updateMessages()} call, so no additional synchronisation is needed.
 */
public class SmartWorkflowChatModelListener implements ChatModelListener {

  /** Metadata captured from one LLM response. */
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
    var usage = response.tokenUsage();
    var fn = response.finishReason();
    pending = new ResponseMetadata(
        usage != null ? usage.inputTokenCount() : null,
        usage != null ? usage.outputTokenCount() : null,
        usage != null ? usage.totalTokenCount() : null,
        fn != null ? fn.name() : null,
        response.modelName(),
        durationMs);
  }

  /**
   * Returns and clears the metadata buffered from the most recent LLM response.
   * Called by {@link IvyChatMemoryStore#updateMessages} immediately after an
   * AI message has been appended to memory.
   */
  public ResponseMetadata drainPending() {
    var m = pending;
    pending = null;
    return m;
  }
}
