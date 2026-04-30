package com.axonivy.utils.smart.workflow.memory.store;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.ImageContent.DetailLevel;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.data.pdf.PdfFile;
import dev.langchain4j.data.video.Video;

public class MessageSerializer {

  static final ObjectMapper MAPPER = createMapper();

  public static List<ChatMessage> read(String json) {
    try {
      return MAPPER.readValue(json, Messages.class).messages;
    } catch (Exception ex) {
      throw new RuntimeException("Failed to deserialize messages", ex);
    }
  }

  public static String write(List<ChatMessage> messages) {
    try {
      Messages msgs = new Messages(messages);
      return MAPPER.writeValueAsString(msgs);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to serialize messages", ex);
    }
  }

  private static ObjectMapper createMapper() {
    var mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerSubtypes(
        new NamedType(UserMessage.class, UserMessage.class.getSimpleName()),
        new NamedType(SystemMessage.class, SystemMessage.class.getSimpleName()),
        new NamedType(AiMessage.class, AiMessage.class.getSimpleName()),
        new NamedType(ToolExecutionResultMessage.class, ToolExecutionResultMessage.class.getSimpleName()),
        new NamedType(CustomMessage.class, CustomMessage.class.getSimpleName()),

        new NamedType(TextContent.class, TextContent.class.getSimpleName()),
        new NamedType(ImageContent.class, ImageContent.class.getSimpleName()),
        new NamedType(AudioContent.class, AudioContent.class.getSimpleName()),
        new NamedType(PdfFileContent.class, PdfFileContent.class.getSimpleName()),
        new NamedType(VideoContent.class, VideoContent.class.getSimpleName()));

    mapper.addMixIn(ChatMessage.class, MessageMixIn.class);
    mapper.addMixIn(UserMessage.class, UserMessageMixIn.class);
    mapper.addMixIn(UserMessage.Builder.class, UserMessageBuilderMixIn.class);
    mapper.addMixIn(SystemMessage.class, SystemMessageMixIn.class);
    mapper.addMixIn(CustomMessage.class, CustomMessageMixIn.class);

    mapper.addMixIn(AiMessage.class, AiMessageMixIn.class);
    mapper.addMixIn(AiMessage.Builder.class, AiMessageBuilderMixIn.class);
    mapper.addMixIn(ToolExecutionRequest.class, ToolExecutionRequestMixIn.class);
    mapper.addMixIn(ToolExecutionRequest.Builder.class, ToolExecutionRequestBuilderMixIn.class);
    mapper.addMixIn(ToolExecutionResultMessage.class, ToolExecutionResultMessageMixIn.class);
    mapper.addMixIn(ToolExecutionResultMessage.Builder.class, ToolExecutionResultMessageBuilderMixIn.class);

    mapper.addMixIn(Content.class, ContentMixIn.class);
    mapper.addMixIn(TextContent.class, TextContentMixIn.class);
    mapper.addMixIn(ImageContent.class, ImageContentMixIn.class);
    mapper.addMixIn(Image.class, ImageMixIn.class);
    mapper.addMixIn(Image.Builder.class, ImageBuilderMixIn.class);
    mapper.addMixIn(AudioContent.class, AudioContentMixIn.class);
    mapper.addMixIn(Audio.class, AudioMixIn.class);
    mapper.addMixIn(Audio.Builder.class, AudioBuilderMixIn.class);
    mapper.addMixIn(PdfFileContent.class, PdfFileContentMixIn.class);
    mapper.addMixIn(PdfFile.class, PdfFileMixIn.class);
    mapper.addMixIn(PdfFile.Builder.class, PdfFileBuilderMixIn.class);
    mapper.addMixIn(VideoContent.class, VideoContentMixIn.class);
    mapper.addMixIn(Video.class, VideoMixIn.class);
    mapper.addMixIn(Video.Builder.class, VideoBuilderMixIn.class);

    return mapper;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  private abstract static class MessageMixIn {
  }

  private abstract static class SystemMessageMixIn {
    @JsonCreator
    SystemMessageMixIn(@JsonProperty("text") String text) {
    }
  }

  private abstract static class CustomMessageMixIn {
    @JsonCreator
    CustomMessageMixIn(@JsonProperty("attributes") Map<String, String> attributes) {
    }
  }

  @JsonDeserialize(builder = UserMessage.Builder.class)
  private abstract static class UserMessageMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class UserMessageBuilderMixIn {
  }

  @JsonDeserialize(builder = AiMessage.Builder.class)
  private abstract static class AiMessageMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class AiMessageBuilderMixIn {
  }

  @JsonDeserialize(builder = ToolExecutionRequest.Builder.class)
  private abstract static class ToolExecutionRequestMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class ToolExecutionRequestBuilderMixIn {
  }

  @JsonDeserialize(builder = ToolExecutionResultMessage.Builder.class)
  private abstract static class ToolExecutionResultMessageMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class ToolExecutionResultMessageBuilderMixIn {
    @JsonProperty("contents")
    abstract ToolExecutionResultMessage.Builder contents(List<Content> contents);

    @JsonIgnore
    abstract ToolExecutionResultMessage.Builder contents(Content... contents);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  private abstract static class ContentMixIn {
  }

  private abstract static class TextContentMixIn {
    @JsonCreator
    TextContentMixIn(@JsonProperty("text") String text) {
    }
  }

  private abstract static class ImageContentMixIn {
    @JsonCreator
    ImageContentMixIn(@JsonProperty("image") Image image, @JsonProperty("detailLevel") DetailLevel detailLevel) {
    }
  }

  @JsonDeserialize(builder = Image.Builder.class)
  private abstract static class ImageMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class ImageBuilderMixIn {
  }

  private abstract static class AudioContentMixIn {
    @JsonCreator
    AudioContentMixIn(@JsonProperty("audio") Audio audio) {
    }
  }

  @JsonDeserialize(builder = Audio.Builder.class)
  private abstract static class AudioMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class AudioBuilderMixIn {
  }

  private abstract static class PdfFileContentMixIn {
    @JsonCreator
    PdfFileContentMixIn(@JsonProperty("pdfFile") PdfFile pdfFile) {
    }
  }

  @JsonDeserialize(builder = PdfFile.Builder.class)
  private abstract static class PdfFileMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class PdfFileBuilderMixIn {
  }

  private abstract static class VideoContentMixIn {
    @JsonCreator
    VideoContentMixIn(@JsonProperty("video") Video video) {
    }
  }

  @JsonDeserialize(builder = Video.Builder.class)
  private abstract static class VideoMixIn {
  }

  @JsonPOJOBuilder
  private abstract static class VideoBuilderMixIn {
  }

  private static class Messages {
    private List<ChatMessage> messages;

    @JsonCreator
    public Messages(@JsonProperty("messages") List<ChatMessage> messages) {
      this.messages = messages;
    }
  }
}
