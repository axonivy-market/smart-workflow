package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.listener.ChatHistoryRecordingListener;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

@IvyTest class TestChatHistoryRepository {

  private static final String CASE_UUID = "test-case-uuid";
  private static final String TASK_UUID = "test-task-uuid";

  private final List<ChatHistoryEntry> fakeRepo = new ArrayList<>();

  @BeforeEach
  void setUp() {
    fakeRepo.clear();
  }

  private ChatHistoryRepository newStore(ChatHistoryRecordingListener listener) {
    return newStore(listener, CASE_UUID, TASK_UUID);
  }

  private ChatHistoryRepository newStore(ChatHistoryRecordingListener listener, String caseUuid, String taskUuid) {
    return new ChatHistoryRepository(listener, caseUuid, taskUuid,
        () -> fakeRepo.stream()
            .filter(e -> caseUuid.equals(e.getCaseUuid()) && taskUuid.equals(e.getTaskUuid())).toList(),
        entry -> {
          fakeRepo.removeIf(e -> e.getCaseUuid().equals(entry.getCaseUuid()) && e.getTaskUuid().equals(entry.getTaskUuid()));
          fakeRepo.add(entry);
        },
        fakeRepo::remove);
  }

  private Optional<ChatHistoryEntry> findEntry(String caseUuid, String taskUuid) {
    return fakeRepo.stream()
        .filter(e -> caseUuid.equals(e.getCaseUuid()) && taskUuid.equals(e.getTaskUuid()))
        .findFirst();
  }

  @Test
  void updateMessagesPersistsEntryWithCorrectMetadata() {
    var store = newStore(null);

    store.updateMessages( List.of(UserMessage.from("Hello")));

    var entry = findEntry(CASE_UUID, TASK_UUID);
    assertThat(entry).isPresent();
    assertThat(entry.get().getCaseUuid()).isEqualTo(CASE_UUID);
    assertThat(entry.get().getTaskUuid()).isEqualTo(TASK_UUID);
    assertThat(entry.get().getLastUpdated()).isNotNull();
  }

  @Test
  void tokenUsageCapturedOnlyWhenLastMessageIsAi() {
    var listener = new ChatHistoryRecordingListener();
    var storeAi = newStore(listener, CASE_UUID, "task-ai");
    var storeUser = newStore(listener, CASE_UUID, "task-user");

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    storeAi.updateMessages(List.of(
        UserMessage.from("Hello"),
        AiMessage.aiMessage("Response")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 10));
    storeUser.updateMessages(List.of(UserMessage.from("Hello")));

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
    var listener = new ChatHistoryRecordingListener();
    var store = newStore(listener);

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    store.updateMessages( List.of(
        UserMessage.from("First"),
        AiMessage.aiMessage("First reply")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 15));
    store.updateMessages( List.of(
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
