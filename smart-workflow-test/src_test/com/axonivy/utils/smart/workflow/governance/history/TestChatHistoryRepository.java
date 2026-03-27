package com.axonivy.utils.smart.workflow.governance.history;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.listener.AgentResponseListener;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;

public class TestChatHistoryRepository {

  private InMemoryHistoryStorage storage;
  private AgentResponseListener listener;

  @BeforeEach
  void setUp() {
    storage = new InMemoryHistoryStorage();
    listener = new AgentResponseListener(new ChatHistoryRepository("case-1", "task-1", "test-agent", "test-element", "test-process", storage));
  }

  @Test
  void storesMessagesAndMetadata() {
    listener.onEvent(buildEvent("It's so hot", "chat", 5, 10, "Head to Lake Lucerne, it's refreshing!"));

    assertThat(storage.findAll()).hasSize(1);
    var entry = storage.findAll().get(0);

    assertThat(entry.getCaseUuid()).isEqualTo("case-1");
    assertThat(entry.getTaskUuid()).isEqualTo("task-1");
    assertThat(entry.getLastUpdated()).isNotNull();

    var messages = ChatMessageDeserializer.messagesFromJson(entry.getMessagesJson());
    var userMessage = messages.stream()
        .filter(UserMessage.class::isInstance).map(UserMessage.class::cast)
        .findFirst();
    assertThat(userMessage).isPresent();
    assertThat(userMessage.get().singleText()).contains("It's so hot");

    var aiMessage = messages.stream()
        .filter(AiMessage.class::isInstance).map(AiMessage.class::cast)
        .findFirst();
    assertThat(aiMessage).isPresent();
    assertThat(aiMessage.get().text()).isEqualTo("Head to Lake Lucerne, it's refreshing!");

    assertThat(entry.getTokenUsageJson())
        .contains("\"inputTokens\":5")
        .contains("\"outputTokens\":10")
        .contains("\"aiServiceMethod\":\"chat\"")
        .contains("\"toolNames\":[]");
  }

  private AiServiceResponseReceivedEvent buildEvent(String userText, String methodName,
      int inputTokens, int outputTokens, String aiText) {
    var invocationCtx = InvocationContext.builder()
        .invocationId(UUID.randomUUID())
        .timestamp(Instant.now())
        .methodName(methodName)
        .interfaceName("ChatAgent")
        .build();
    var request = ChatRequest.builder().messages(UserMessage.from(userText)).build();
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage(aiText))
        .tokenUsage(new TokenUsage(inputTokens, outputTokens))
        .modelName("test-model")
        .build();
    return AiServiceResponseReceivedEvent.builder()
        .invocationContext(invocationCtx)
        .request(request)
        .response(response)
        .build();
  }
}
