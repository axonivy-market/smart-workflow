package com.axonivy.utils.smart.workflow.governance.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

@IvyTest class TestSmartWorkflowChatMemoryStore {

  private static final String AGENT_ID = "test-agent";
  private static final String CASE_UUID = "test-case-uuid";

  @Test
  void updateAndGetMessagesRoundTrip() {
    var store = new SmartWorkflowChatMemoryStore(AGENT_ID, null, CASE_UUID);
    assertThat(store.getMessages("mem-roundtrip")).isEmpty();

    List<ChatMessage> messages = List.of(
        UserMessage.from("Hello"),
        AiMessage.aiMessage("Hi there!")
    );
    store.updateMessages("mem-roundtrip", messages);
    var retrieved = store.getMessages("mem-roundtrip");

    assertThat(retrieved).hasSize(2);
    assertThat(retrieved.get(0)).isInstanceOf(UserMessage.class);
    assertThat(retrieved.get(1)).isInstanceOf(AiMessage.class);
  }

  @Test
  void updateMessagesPersistsEntryWithCorrectMetadata() {
    var store = new SmartWorkflowChatMemoryStore(AGENT_ID, null, CASE_UUID);

    store.updateMessages("mem-metadata", List.of(UserMessage.from("Hello")));

    var entries = Ivy.repo().search(ChatMemoryEntry.class)
        .textField("memoryId").isEqualToIgnoringCase("mem-metadata")
        .execute().getAll();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getAgentId()).isEqualTo(AGENT_ID);
    assertThat(entries.get(0).getCaseUuid()).isEqualTo(CASE_UUID);
    assertThat(entries.get(0).getLastUpdated()).isNotNull();
  }

  @Test
  void deleteMessagesRemovesEntry() {
    var store = new SmartWorkflowChatMemoryStore(AGENT_ID, null, CASE_UUID);
    store.updateMessages("mem-delete", List.of(UserMessage.from("Hello")));

    store.deleteMessages("mem-delete");

    assertThat(store.getMessages("mem-delete")).isEmpty();
  }

  @Test
  void tokenUsageCapturedOnlyWhenLastMessageIsAi() {
    var listener = new SmartWorkflowChatModelListener();
    var store = new SmartWorkflowChatMemoryStore(AGENT_ID, listener, CASE_UUID);

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    store.updateMessages("mem-token-ai", List.of(
        UserMessage.from("Hello"),
        AiMessage.aiMessage("Response")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 10));
    store.updateMessages("mem-token-user", List.of(UserMessage.from("Hello")));

    var aiEntry = Ivy.repo().search(ChatMemoryEntry.class)
        .textField("memoryId").isEqualToIgnoringCase("mem-token-ai")
        .execute().getAll().get(0);
    assertThat(aiEntry.getTokenUsageJson())
        .isNotBlank()
        .contains("\"inputTokens\":10")
        .contains("\"outputTokens\":20");

    var userEntry = Ivy.repo().search(ChatMemoryEntry.class)
        .textField("memoryId").isEqualToIgnoringCase("mem-token-user")
        .execute().getAll().get(0);
    assertThat(userEntry.getTokenUsageJson()).isNullOrEmpty();
  }

  @Test
  void updateMessagesAppendsMultipleTokenUsageEntries() {
    var listener = new SmartWorkflowChatModelListener();
    var store = new SmartWorkflowChatMemoryStore(AGENT_ID, listener, CASE_UUID);

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(10, 20));
    store.updateMessages("mem-token-multi", List.of(
        UserMessage.from("First"),
        AiMessage.aiMessage("First reply")
    ));

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(5, 15));
    store.updateMessages("mem-token-multi", List.of(
        UserMessage.from("First"),
        AiMessage.aiMessage("First reply"),
        UserMessage.from("Second"),
        AiMessage.aiMessage("Second reply")
    ));

    var entries = Ivy.repo().search(ChatMemoryEntry.class)
        .textField("memoryId").isEqualToIgnoringCase("mem-token-multi")
        .execute().getAll();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getTokenUsageJson())
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
