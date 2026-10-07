package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.Role;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

@IvyTest
public class TestMessageViewModelParser {

  @Test
  void parseByRole_nullEntry_returnsEmptyMap() {
    var result = new MessageViewModelParser().parseByRole(null);
    assertThat(result).isEmpty();
  }

  @Test
  void parseByRole_nullMessagesJson_returnsEmptyMap() {
    var result = new MessageViewModelParser().parseByRole(new AgentConversationEntry());
    assertThat(result).isEmpty();
  }

  @Test
  void parseByRole_systemMessage_groupedUnderSystem() {
    var entry = entry(SystemMessage.from("sys"));
    var result = new MessageViewModelParser().parseByRole(entry);
    assertThat(result.get(Role.SYSTEM)).hasSize(1);
    assertThat(result.get(Role.SYSTEM).get(0).getText()).isEqualTo("sys");
  }

  @Test
  void parseByRole_userAndAssistant_groupedCorrectly() {
    var entry = entry(UserMessage.from("q"), AiMessage.from("a"));
    var result = new MessageViewModelParser().parseByRole(entry);
    assertThat(result.get(Role.USER)).hasSize(1);
    assertThat(result.get(Role.ASSISTANT)).hasSize(1);
  }

  @Test
  void parseByRole_sameEntryCalledTwice_returnsCachedResult() {
    var parser = new MessageViewModelParser();
    var entry = entry(UserMessage.from("hello"));
    var first = parser.parseByRole(entry);
    var second = parser.parseByRole(entry);
    assertThat(first).isSameAs(second);
  }

  @Test
  void parseByRole_differentAgentsSameTaskUuid_separateCacheEntries() {
    var parser = new MessageViewModelParser();
    var entryA = entry("case-1", "task-1", "agent-a", UserMessage.from("From A"));
    var entryB = entry("case-1", "task-1", "agent-b", UserMessage.from("From B"));

    var resultA = parser.parseByRole(entryA);
    var resultB = parser.parseByRole(entryB);

    assertThat(resultA.get(Role.USER).get(0).getText()).isEqualTo("From A");
    assertThat(resultB.get(Role.USER).get(0).getText()).isEqualTo("From B");
  }

  private static AgentConversationEntry entry(String caseUuid, String taskUuid, String agentId,
      ChatMessage... msgs) {
    var e = new AgentConversationEntry();
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setAgentId(agentId);
    e.setMessagesJson(ChatMessageSerializer.messagesToJson(List.of(msgs)));
    return e;
  }

  private static AgentConversationEntry entry(ChatMessage... msgs) {
    return entry("case-1", "task-1", "agent-1", msgs);
  }
}
