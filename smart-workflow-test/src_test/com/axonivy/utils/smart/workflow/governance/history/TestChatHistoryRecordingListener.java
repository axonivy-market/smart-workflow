package com.axonivy.utils.smart.workflow.governance.history;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.listener.AbstractChatModelListener;
import com.axonivy.utils.smart.workflow.governance.listener.ChatHistoryRecordingListener;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

public class TestChatHistoryRecordingListener {

  private ChatHistoryRecordingListener listener;

  @BeforeEach
  void setUp() {
    listener = new ChatHistoryRecordingListener();
  }

  @Test
  void drainPendingClearsState() {
    assertThat(listener.drainPending()).isNull();

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(100, 50));
    listener.drainPending();

    assertThat(listener.drainPending()).isNull();
  }

  @Test
  void onResponseCapturesAllMetadata() {
    listener.onRequest(null);
    listener.onResponse(buildResponseContext(100, 50));

    AbstractChatModelListener.ResponseMetadata meta = listener.drainPending();

    assertThat(meta).isNotNull();
    assertThat(meta.inputTokens()).isEqualTo(100);
    assertThat(meta.outputTokens()).isEqualTo(50);
    assertThat(meta.totalTokens()).isEqualTo(150);
    assertThat(meta.modelName()).isEqualTo("test-model");
    assertThat(meta.durationMs()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void onResponseWithNullTokenUsageReturnsNullTokenFields() {
    listener.onRequest(null);
    listener.onResponse(buildResponseContextNoTokens());

    AbstractChatModelListener.ResponseMetadata meta = listener.drainPending();

    assertThat(meta).isNotNull();
    assertThat(meta.inputTokens()).isNull();
    assertThat(meta.outputTokens()).isNull();
    assertThat(meta.totalTokens()).isNull();
  }

  private ChatModelResponseContext buildResponseContext(int inputTokens, int outputTokens) {
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("test response"))
        .tokenUsage(new TokenUsage(inputTokens, outputTokens))
        .modelName("test-model")
        .build();
    var request = ChatRequest.builder().messages(UserMessage.from("Hello")).build();
    return new ChatModelResponseContext(response, request, null, Map.of());
  }

  private ChatModelResponseContext buildResponseContextNoTokens() {
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("test response"))
        .modelName("test-model")
        .build();
    var request = ChatRequest.builder().messages(UserMessage.from("Hello")).build();
    return new ChatModelResponseContext(response, request, null, Map.of());
  }
}
