package com.axonivy.utils.smart.workflow.memory.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

@IvyProcessTest
class TestBusinessDataMemory {

  private String id;
  private BusinessDataMemory memory;

  @BeforeEach
  void setUp(){
    id = "testId";
    memory = new BusinessDataMemory();
  }
  
  @Test
  void create() {
    assertThat(memory.getMessages(id))
      .isEmpty();
    memory.updateMessages(id, List.of(new UserMessage(List.of(new TextContent("hey there")))));

    assertThat(memory.getMessages(id))
      .hasSize(1);
  }

  @Test
  void clean() {
    memory.updateMessages(id, List.of(new UserMessage(List.of(new TextContent("hey there")))));
    assertThat(memory.getMessages(id))
        .hasSize(1);
    memory.deleteMessages(id);
    assertThat(memory.getMessages(id))
        .isEmpty();
  }

  @Test
  void update() {
    assertThat(memory.getMessages(id))
        .isEmpty();
    var user = new UserMessage(List.of(new TextContent("hey there")));
    memory.updateMessages(id, List.of(user));
    assertThat(memory.getMessages(id))
        .hasSize(1);

    var system = new SystemMessage("Please to meet you");
    memory.updateMessages(id, List.of(user, system));
    assertThat(memory.getMessages(id))
      .extracting(dev.langchain4j.data.message.ChatMessage::type)
      .containsExactly(ChatMessageType.USER, ChatMessageType.SYSTEM);
  }

}
