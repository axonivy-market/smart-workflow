package com.axonivy.utils.smart.workflow.memory.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

@IvyTest
class TestMessageSerializer {

  @Test
  void readWrite() {
    var user = new UserMessage(List.of(new TextContent("hey there")));
    var json = MessageSerializer.write(List.of(user));

    var messages = MessageSerializer.read(json);
    assertThat(messages).hasSize(1);
    var msg = messages.get(0);
    assertThat((UserMessage)msg).isEqualTo(user); 
  }

  @Test
  void readWrite_system() {
    var system = new SystemMessage("systematic");
    var json = MessageSerializer.write(List.of(system));

    var messages = MessageSerializer.read(json);
    assertThat(messages).hasSize(1);
    var msg = messages.get(0);
    assertThat((SystemMessage) msg).isEqualTo(system);
  }

}
