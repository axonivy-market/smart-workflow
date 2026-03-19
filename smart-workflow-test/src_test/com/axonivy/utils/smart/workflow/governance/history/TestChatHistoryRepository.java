package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

@IvyProcessTest
class TestChatHistoryRepository {

  private static final BpmProcess TEST_TOOL_USER = BpmProcess.name("TestToolUser");

  private InMemoryHistoryStorage storage;

  @BeforeEach
  void setup(AppFixture fixture) {
    storage = new InMemoryHistoryStorage();
    ChatHistoryRepository.testStorage = storage;
    fixture.var(AiConf.DEFAULT_PROVIDER, DummyChatModelProvider.NAME);
    fixture.var("AI.History.Enabled", "true");
    DummyChatModelProvider.defineChat(req -> ChatResponse.builder()
        .aiMessage(AiMessage.aiMessage("Head to Lake Lucerne, it's refreshing!"))
        .tokenUsage(new TokenUsage(5, 10))
        .build());
  }

  @AfterEach
  void teardown() {
    ChatHistoryRepository.testStorage = null;
  }

  @Test
  void historyIsRecordedAfterAgentCall(BpmClient client) {
    client.start()
        .process(TEST_TOOL_USER.elementName("systemMessage"))
        .execute();

    var entries = storage.findAll();
    assertThat(entries).hasSize(1);
    var entry = entries.get(0);

    assertThat(entry.getCaseUuid()).isNotBlank();
    assertThat(entry.getTaskUuid()).isNotBlank();
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
}
