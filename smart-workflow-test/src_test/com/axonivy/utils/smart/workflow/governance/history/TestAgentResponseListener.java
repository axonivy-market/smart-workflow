package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.listener.AgentResponseListener;
import com.axonivy.utils.smart.workflow.governance.history.recorder.HistoryRecorder;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;

public class TestAgentResponseListener {

  private List<List<ChatMessage>> capturedMessages;
  private List<HistoryRecorder.ResponseMetadata> capturedMetadata;
  private AgentResponseListener listener;

  @BeforeEach
  void setUp() {
    capturedMessages = new ArrayList<>();
    capturedMetadata = new ArrayList<>();
    listener = new AgentResponseListener(
        (messages, metadata) -> {
          capturedMessages.add(messages);
          capturedMetadata.add(metadata);
        });
  }

  @Test
  void capturesMetadata() {
    listener.onEvent(buildEvent("Hello", "chat", 100, 50, 0.7));

    assertThat(capturedMetadata).hasSize(1);
    var meta = capturedMetadata.get(0);
    assertThat(meta.inputTokens()).isEqualTo(100);
    assertThat(meta.outputTokens()).isEqualTo(50);
    assertThat(meta.totalTokens()).isEqualTo(150);
    assertThat(meta.modelName()).isEqualTo("test-model");
    assertThat(meta.durationMs()).isGreaterThanOrEqualTo(0L);
    assertThat(meta.aiServiceMethod()).isEqualTo("chat");
    assertThat(meta.toolNames()).isEmpty();
    assertThat(meta.temperature()).isEqualTo(0.7);

    var responseNoTokens = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("test response"))
        .modelName("test-model")
        .build();
    listener.onEvent(buildEvent("Hi", "chat", responseNoTokens, null));

    var nullMeta = capturedMetadata.get(1);
    assertThat(nullMeta.inputTokens()).isNull();
    assertThat(nullMeta.outputTokens()).isNull();
    assertThat(nullMeta.totalTokens()).isNull();
    assertThat(nullMeta.temperature()).isNull();
  }

  private AiServiceResponseReceivedEvent buildEvent(String userText, String methodName, int inputTokens,
      int outputTokens) {
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("test response"))
        .tokenUsage(new TokenUsage(inputTokens, outputTokens))
        .modelName("test-model")
        .build();
    return buildEvent(userText, methodName, response, null);
  }

  private AiServiceResponseReceivedEvent buildEvent(String userText, String methodName, int inputTokens,
      int outputTokens, Double temperature) {
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("test response"))
        .tokenUsage(new TokenUsage(inputTokens, outputTokens))
        .modelName("test-model")
        .build();
    return buildEvent(userText, methodName, response, temperature);
  }

  private AiServiceResponseReceivedEvent buildEvent(String userText, String methodName, ChatResponse response, Double temperature) {
    var requestParams = ChatRequestParameters.builder();
    if (temperature != null) {
      requestParams.temperature(temperature);
    }
    var invocationCtx = InvocationContext.builder()
        .invocationId(UUID.randomUUID())
        .timestamp(Instant.now())
        .methodName(methodName)
        .interfaceName("ChatAgent")
        .defaultRequestParameters(requestParams.build())
        .build();
    var request = ChatRequest.builder().messages(UserMessage.from(userText)).build();
    return AiServiceResponseReceivedEvent.builder()
        .invocationContext(invocationCtx)
        .request(request)
        .response(response)
        .build();
  }
}
