package com.axonivy.utils.smart.workflow.governance.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.listener.SmartWorkflowChatModelListener;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

@IvyTest class TestSmartWorkflowChatMemoryStore {

  private static final String CASE_UUID = "test-case-uuid";
  private static final String TASK_UUID = "test-task-uuid";

  private final List<ChatMemoryEntry> fakeRepo = new ArrayList<>();

  @BeforeEach
  void setUp() {
    fakeRepo.clear();
  }

  private SmartWorkflowChatMemoryStore newStore(SmartWorkflowChatModelListener listener) {
    return newStore(listener, CASE_UUID, TASK_UUID);
  }

  private SmartWorkflowChatMemoryStore newStore(SmartWorkflowChatModelListener listener, String caseUuid, String taskUuid) {
    return new SmartWorkflowChatMemoryStore(listener, caseUuid, taskUuid,
        () -> fakeRepo.stream()
            .filter(e -> caseUuid.equals(e.getCaseUuid()) && taskUuid.equals(e.getTaskUuid())).toList(),
        entry -> {
          fakeRepo.removeIf(e -> e.getCaseUuid().equals(entry.getCaseUuid()) && e.getTaskUuid().equals(entry.getTaskUuid()));
          fakeRepo.add(entry);
        },
        fakeRepo::remove);
  }

  private Optional<ChatMemoryEntry> findEntry(String caseUuid, String taskUuid) {
    return fakeRepo.stream()
        .filter(e -> caseUuid.equals(e.getCaseUuid()) && taskUuid.equals(e.getTaskUuid()))
        .findFirst();
  }

  @Test
  void updateAndGetMessagesRoundTrip() {
    var store = newStore(null);
    assertThat(store.getMessages(null)).isEmpty();

    List<ChatMessage> messages = List.of(
        UserMessage.from("Hello"),
        AiMessage.aiMessage("Hi there!")
    );
    store.updateMessages(null, messages);
    var retrieved = store.getMessages(null);

    assertThat(retrieved).hasSize(2);
    assertThat(retrieved.get(0)).isInstanceOf(UserMessage.class);
    assertThat(retrieved.get(1)).isInstanceOf(AiMessage.class);
  }

  @Test
  void updateMessagesPersistsEntryWithCorrectMetadata() {
    var store = newStore(null);

    store.updateMessages(null, List.of(UserMessage.from("Hello")));

    var entry = findEntry(CASE_UUID, TASK_UUID);
    assertThat(entry).isPresent();
    assertThat(entry.get().getCaseUuid()).isEqualTo(CASE_UUID);
    assertThat(entry.get().getTaskUuid()).isEqualTo(TASK_UUID);
    assertThat(entry.get().getLastUpdated()).isNotNull();
  }

  @Test
  void deleteMessagesRemovesEntry() {
    var store = newStore(null);
    store.updateMessages(null, List.of(UserMessage.from("Hello")));

    store.deleteMessages(null);

    assertThat(store.getMessages(null)).isEmpty();
  }

  @Test
  void tokenUsageCapturedOnlyWhenLastMessageIsAi() {
    var listener = new SmartWorkflowChatModelListener();
    var storeAi = newStore(listener, CASE_UUID, "task-ai");
    var storeUser = newStore(listener, CASE_UUID, "task-user");

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    storeAi.updateMessages(null, List.of(
        UserMessage.from("Hello"),
        AiMessage.aiMessage("Response")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 10));
    storeUser.updateMessages(null, List.of(UserMessage.from("Hello")));

    var aiEntry = findEntry(CASE_UUID, "task-ai");
    assertThat(aiEntry).isPresent();
    assertThat(aiEntry.get().getTokenUsageJson())
        .isNotBlank()
        .contains("\"inputTokens\":10")
        .contains("\"outputTokens\":20");

    var userEntry = findEntry(CASE_UUID, "task-user");
    assertThat(userEntry).isPresent();
    assertThat(userEntry.get().getTokenUsageJson()).isNullOrEmpty();
  }

  @Test
  void updateMessagesAppendsMultipleTokenUsageEntries() {
    var listener = new SmartWorkflowChatModelListener();
    var store = newStore(listener);

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    store.updateMessages(null, List.of(
        UserMessage.from("First"),
        AiMessage.aiMessage("First reply")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 15));
    store.updateMessages(null, List.of(
        UserMessage.from("First"),
        AiMessage.aiMessage("First reply"),
        UserMessage.from("Second"),
        AiMessage.aiMessage("Second reply")
    ));

    var entry = findEntry(CASE_UUID, TASK_UUID);
    assertThat(entry).isPresent();
    assertThat(entry.get().getTokenUsageJson())
        .contains("\"inputTokens\":10")
        .contains("\"inputTokens\":5");
  }

  private ChatModelResponseContext buildResponseContext(int inputTokens, int outputTokens) {
    var response = ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("response"))
        .tokenUsage(new TokenUsage(inputTokens, outputTokens))
        .modelName("test-model")
        .build();
    var request = ChatRequest.builder().messages(UserMessage.from("Hello")).build();
    return new ChatModelResponseContext(response, request, null, Map.of());
  }
}
