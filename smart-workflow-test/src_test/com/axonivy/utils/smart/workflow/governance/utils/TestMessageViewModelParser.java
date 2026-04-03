package com.axonivy.utils.smart.workflow.governance.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.Role;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestMessageViewModelParser {

  private final MessageViewModelParser parser = new MessageViewModelParser();

  private static AgentConversationEntry entryWithMessages(String messagesJson) {
    var entry = new AgentConversationEntry();
    entry.setCaseUuid("case-1");
    entry.setTaskUuid("task-1");
    entry.setMessagesJson(messagesJson);
    return entry;
  }

  private static final String MIXED_MSGS = """
      [
        {"type":"SYSTEM","text":"System prompt"},
        {"type":"USER","contents":[{"type":"TEXT","text":"User question"}]},
        {"type":"AI","text":"AI answer","toolExecutionRequests":[]},
        {"type":"TOOL_EXECUTION_RESULT","id":"call-1","toolName":"myTool","text":"tool result"}
      ]
      """;

  @Test
  void parseInvalidInputEmpty() {
    assertThat(parser.parse(null)).isEmpty();
    assertThat(parser.parse(entryWithMessages(null))).isEmpty();
    assertThat(parser.parse(entryWithMessages("not-valid-json"))).isEmpty();
  }

  @Test
  void parseMixedMessagesAllRoles() {
    var result = parser.parse(entryWithMessages(MIXED_MSGS));
    assertThat(result).extracting(MessageViewModel::getRole)
        .containsExactly(Role.SYSTEM, Role.USER, Role.ASSISTANT, Role.TOOL);
    assertThat(result.get(0).getText()).isEqualTo("System prompt");
    assertThat(result.get(1).getText()).isEqualTo("User question");
    assertThat(result.get(2).getText()).isEqualTo("AI answer");
    assertThat(result.get(3).getText()).contains("myTool").contains("tool result");
  }

  @Test
  void getFilteredMessagesRole() {
    var entry = entryWithMessages(MIXED_MSGS);
    assertThat(parser.getSystemMessages(entry)).hasSize(1)
        .allMatch(m -> m.getRole() == Role.SYSTEM);
    assertThat(parser.getUserMessages(entry)).hasSize(1)
        .allMatch(m -> m.getRole() == Role.USER);
    assertThat(parser.getAssistantMessages(entry)).hasSize(1)
        .allMatch(m -> m.getRole() == Role.ASSISTANT);
  }

  @Test
  void messageViewModelNullText() {
    var vm = new MessageViewModel(Role.ASSISTANT, null);
    assertThat(vm.getText()).isEqualTo("");
  }
}
