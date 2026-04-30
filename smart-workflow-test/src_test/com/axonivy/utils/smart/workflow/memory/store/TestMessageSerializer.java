package com.axonivy.utils.smart.workflow.memory.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.ImageContent.DetailLevel;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

@IvyTest
class TestMessageSerializer {

  @Test
  void readWrite_allContents() {
    var user = new UserMessage(List.of(
      new TextContent("hey there"),
      new ImageContent("file://path/to/image.jpg", DetailLevel.LOW),
      new AudioContent("file://path/to/audio.mp3"),
      new PdfFileContent("file://path/to/doc.pdf"),
      new VideoContent("file://path/to/video.mp4")
    ));
    var json = MessageSerializer.write(List.of(user));
    assertThat(json)
      .contains("hey there")
      .contains("image.jpg")
      .contains("audio.mp3")
      .contains("doc.pdf")
      .contains("video.mp4");

    var messages = MessageSerializer.read(json);
    assertThat(messages).hasSize(1);
    var msg = messages.get(0);
    assertThat((UserMessage)msg).isEqualTo(user); 
  }

  @Test
  void readWrite_system() {
    var system = new SystemMessage("systematic");
    var json = MessageSerializer.write(List.of(system));
    assertThat(json).isEqualTo("[{\"text\":\"systematic\",\"type\":\"SYSTEM\"}]");

    var messages = MessageSerializer.read(json);
    assertThat(messages).hasSize(1);
    var msg = messages.get(0);
    assertThat((SystemMessage) msg).isEqualTo(system);
  }

  @Test
  void readWrite_userOnly() throws Exception {
    var user = new UserMessage(List.of(new TextContent("hey there")));
    var json = ChatMessageSerializer.messageToJson(user);


    assertThat(json.toString()).isEqualTo("{\"contents\":[{\"text\":\"hey there\",\"type\":\"TEXT\"}],\"type\":\"USER\"}");

    var reloaded = ChatMessageDeserializer.messageFromJson(json.toString());
    assertThat(reloaded).isEqualTo(user);
  }
  
  @Test
  void readWrite_systemOnly() throws Exception {
    var system = new SystemMessage("systematic");
    var json = ChatMessageSerializer.messageToJson(system);
    assertThat(json.toString()).isEqualTo("{\"text\":\"systematic\",\"type\":\"SYSTEM\"}");

    var reloaded = ChatMessageDeserializer.messageFromJson(json.toString());
    assertThat(reloaded).isEqualTo(system);
  }

  @Test
  void readWrite_aiOnly() throws Exception {
    var add = ToolExecutionRequest.builder()
      .id("id123")
      .name("add")
      .arguments("add(1,2)")
      .build();
    var ai = new AiMessage("artificial intelligence", List.of(add));
    var json = ChatMessageSerializer.messageToJson(ai);
    assertThat(json.toString())
      .isEqualTo("{\"text\":\"artificial intelligence\",\"toolExecutionRequests\":[{\"id\":\"id123\",\"name\":\"add\",\"arguments\":\"add(1,2)\"}],\"attributes\":{},\"type\":\"AI\"}");

    var reloaded = ChatMessageDeserializer.messageFromJson(json.toString());
    assertThat(reloaded).isEqualTo(ai);
  }

  @Test
  void readWrite_customOnly() throws Exception {
    var custom = new CustomMessage(Map.of("a","b"));
    var json = ChatMessageSerializer.messageToJson(custom);
    assertThat(json.toString())
        .isEqualTo("{\"attributes\":{\"a\":\"b\"},\"type\":\"CUSTOM\"}");

    var reloaded = ChatMessageDeserializer.messageFromJson(json.toString());
    assertThat(reloaded).isEqualTo(custom);
  }

  @Test
  void readWrite_toolResponseOnly() throws Exception {
    var toolResponse = ToolExecutionResultMessage.builder()
      .id("id123")
      .contents(List.of(new TextContent("the result")))
      .build();
    var json = ChatMessageSerializer.messageToJson(toolResponse);
    assertThat(json.toString())
        .isEqualTo("{\"id\":\"id123\",\"contents\":[{\"text\":\"the result\",\"type\":\"TEXT\"}],\"attributes\":{},\"type\":\"TOOL_EXECUTION_RESULT\"}");

    var reloaded = ChatMessageDeserializer.messageFromJson(json.toString());
    assertThat(reloaded).isEqualTo(toolResponse);
  }
}
