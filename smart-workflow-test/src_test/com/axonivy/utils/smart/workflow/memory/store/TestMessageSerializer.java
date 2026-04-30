package com.axonivy.utils.smart.workflow.memory.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.data.message.ImageContent.DetailLevel;

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
    assertThat(json).isEqualTo("{\"messages\":[{\"type\":\"SystemMessage\",\"text\":\"systematic\"}]}");

    var messages = MessageSerializer.read(json);
    assertThat(messages).hasSize(1);
    var msg = messages.get(0);
    assertThat((SystemMessage) msg).isEqualTo(system);
  }

  @Test
  void readWrite_userOnly() throws Exception {
    var user = new UserMessage(List.of(new TextContent("hey there")));
    var json = MessageSerializer.MAPPER.valueToTree(user);


    assertThat(json.toString()).isEqualTo("{\"type\":\"UserMessage\",\"contents\":[{\"type\":\"TextContent\",\"text\":\"hey there\"}],\"attributes\":{}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), UserMessage.class);
    assertThat(reloaded).isEqualTo(user);
  }


  @Test
  void readWrite_userWrapped() throws Exception {
    var user = new UserMessage(List.of(new TextContent("hey there")));
    var wrapper = new MessageWrapper();
    wrapper.message = user;
    var json = MessageSerializer.MAPPER.valueToTree(wrapper);

    assertThat(json.toString())
        .isEqualTo("{\"message\":{\"type\":\"UserMessage\",\"contents\":[{\"type\":\"TextContent\",\"text\":\"hey there\"}],\"attributes\":{}}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), MessageWrapper.class);
    assertThat(reloaded.message).isEqualTo(user);
  }

  private static class MessageWrapper {
    public ChatMessage message;
  }
  
  @Test
  void readWrite_systemOnly() throws Exception {
    var system = new SystemMessage("systematic");
    var json = MessageSerializer.MAPPER.valueToTree(system);
    assertThat(json.toString()).isEqualTo("{\"type\":\"SystemMessage\",\"text\":\"systematic\"}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), SystemMessage.class);
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
    var json = MessageSerializer.MAPPER.valueToTree(ai);
    assertThat(json.toString())
      .isEqualTo("{\"type\":\"AiMessage\",\"text\":\"artificial intelligence\","+
      "\"toolExecutionRequests\":[{\"id\":\"id123\",\"name\":\"add\",\"arguments\":\"add(1,2)\"}],\"attributes\":{}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), AiMessage.class);
    assertThat(reloaded).isEqualTo(ai);
  }

  @Test
  void readWrite_customOnly() throws Exception {
    var custom = new CustomMessage(Map.of("a","b"));
    var json = MessageSerializer.MAPPER.valueToTree(custom);
    assertThat(json.toString())
        .isEqualTo("{\"type\":\"CustomMessage\",\"attributes\":{\"a\":\"b\"}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), CustomMessage.class);
    assertThat(reloaded).isEqualTo(custom);
  }

  @Test
  void readWrite_toolResponseOnly() throws Exception {
    var toolResponse = ToolExecutionResultMessage.builder()
      .id("id123")
      .contents(List.of(new TextContent("the result")))
      .build();
    var json = MessageSerializer.MAPPER.valueToTree(toolResponse);
    assertThat(json.toString())
        .isEqualTo("{\"type\":\"ToolExecutionResultMessage\",\"id\":\"id123\",\"contents\":[{\"type\":\"TextContent\",\"text\":\"the result\"}],\"attributes\":{}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), ToolExecutionResultMessage.class);
    assertThat(reloaded).isEqualTo(toolResponse);
  }

  @Test
  void readWrite_textContentOnly() throws Exception {
    var textContent = new TextContent("systematic");
    var json = MessageSerializer.MAPPER.valueToTree(textContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"TextContent\",\"text\":\"systematic\"}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), TextContent.class);
    assertThat(reloaded).isEqualTo(textContent);
  }

  @Test
  void readWrite_textContentAnonymous() throws Exception {
    var textContent = (Content)new TextContent("systematic");
    var json = MessageSerializer.MAPPER.valueToTree(textContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"TextContent\",\"text\":\"systematic\"}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), Content.class);
    assertThat(reloaded).isEqualTo(textContent);
  }

  @Test
  void readWrite_textContentAnonymousWrapped() throws Exception {
    var wrapper = new ContentWrapper();
    wrapper.content = new TextContent("systematic");
    var json = MessageSerializer.MAPPER.valueToTree(wrapper);
    assertThat(json.toString()).isEqualTo("{\"content\":{\"type\":\"TextContent\",\"text\":\"systematic\"}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), ContentWrapper.class);
    assertThat(reloaded.content).isEqualTo(wrapper.content);
  }

  private static class ContentWrapper {
    public Content content;
  }

  @Test
  void readWrite_imageContentOnly() throws Exception {
    var imageContent = new ImageContent("file://path/to/image.jpg", DetailLevel.LOW);
    var json = MessageSerializer.MAPPER.valueToTree(imageContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"ImageContent\","+
      "\"image\":{\"url\":\"file://path/to/image.jpg\",\"base64Data\":null,\"mimeType\":null,\"revisedPrompt\":null},"+
      "\"detailLevel\":\"LOW\"}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), ImageContent.class);
    assertThat(reloaded).isEqualTo(imageContent);
  }


  @Test
  void readWrite_audioContentOnly() throws Exception {
    var audioContent = new AudioContent("file://path/to/audio.mp3");
    var json = MessageSerializer.MAPPER.valueToTree(audioContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"AudioContent\","+
      "\"audio\":{\"url\":\"file://path/to/audio.mp3\",\"binaryData\":null,\"base64Data\":null,\"mimeType\":null}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), AudioContent.class);
    assertThat(reloaded).isEqualTo(audioContent);
  }

  @Test
  void readWrite_pdfContentOnly() throws Exception {
    var pdfContent = new PdfFileContent("file://path/to/doc.pdf");
    var json = MessageSerializer.MAPPER.valueToTree(pdfContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"PdfFileContent\"," +
        "\"pdfFile\":{\"url\":\"file://path/to/doc.pdf\",\"base64Data\":null,\"mimeType\":\"application/pdf\"}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), PdfFileContent.class);
    assertThat(reloaded).isEqualTo(pdfContent);
  }

  @Test
  void readWrite_videoContentOnly() throws Exception {
    var videoContent = new VideoContent("file://path/to/video.mp4");
    var json = MessageSerializer.MAPPER.valueToTree(videoContent);
    assertThat(json.toString()).isEqualTo("{\"type\":\"VideoContent\"," +
        "\"video\":{\"url\":\"file://path/to/video.mp4\",\"base64Data\":null,\"mimeType\":null}}");

    var reloaded = MessageSerializer.MAPPER.readValue(json.toString(), VideoContent.class);
    assertThat(reloaded).isEqualTo(videoContent);
  }
}
