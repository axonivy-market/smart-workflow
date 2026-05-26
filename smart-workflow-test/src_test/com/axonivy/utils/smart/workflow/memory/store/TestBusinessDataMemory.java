package com.axonivy.utils.smart.workflow.memory.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

@IvyTest
class TestBusinessDataMemory {

  private static final String ID = "testId";
  private BusinessDataMemory memory;

  @BeforeEach
  void setUp() {
    memory = new BusinessDataMemory();
  }

  @AfterEach
  void tearDown() {
    memory.deleteMessages(ID);
  }

  @Test
  void create() {
    assertThat(memory.getMessages(ID))
        .isEmpty();
    memory.updateMessages(ID, List.of(new UserMessage(List.of(new TextContent("hey there")))));

    assertThat(memory.getMessages(ID))
        .hasSize(1);
  }

  @Test
  void clean() {
    memory.updateMessages(ID, List.of(new UserMessage(List.of(new TextContent("hey there")))));
    assertThat(memory.getMessages(ID))
        .hasSize(1);
    memory.deleteMessages(ID);
    assertThat(memory.getMessages(ID))
        .isEmpty();
  }

  @Test
  void update() {
    assertThat(memory.getMessages(ID))
        .isEmpty();
    var user = new UserMessage(List.of(new TextContent("hey there")));
    memory.updateMessages(ID, List.of(user));
    assertThat(memory.getMessages(ID))
        .hasSize(1);

    var system = new SystemMessage("Pleased to meet you");
    memory.updateMessages(ID, List.of(user, system));
    assertThat(memory.getMessages(ID))
        .extracting(ChatMessage::type)
        .containsExactly(ChatMessageType.USER, ChatMessageType.SYSTEM);
  }

}
