package com.axonivy.utils.smart.workflow.memory.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class MessageSerializer {

static final ObjectMapper MAPPER2 = new ObjectMapper().setVisibility(
      PropertyAccessor.FIELD,
      Visibility.ANY)
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  private static final ObjectMapper MAPPER = new ObjectMapper();
  static {
    MAPPER.activateDefaultTyping(new LaissezFaireSubTypeValidator(),
        ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);
    MAPPER.registerSubtypes(
        new NamedType(UserMessage.class, UserMessage.class.getSimpleName()),
        new NamedType(SystemMessage.class, SystemMessage.class.getSimpleName()),
        new NamedType(AiMessage.class, AiMessage.class.getSimpleName()),
        new NamedType(CustomMessage.class, CustomMessage.class.getSimpleName()),
        new NamedType(ToolExecutionResultMessage.class, ToolExecutionResultMessage.class.getSimpleName()));

    var mod = new SimpleModule();
    // mod.addDeserializer(UserMessage.class, new JsonDeserializer<UserMessage>() {
    //   @Override
    //   public UserMessage deserialize(com.fasterxml.jackson.core.JsonParser p,
    //       DeserializationContext ctxt) throws IOException, JsonMappingException {
    //     var node = p.readValueAsTree();
    //     List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
    //     if (node.get("contents") instanceof ArrayNode contentArray) {
    //       for (var contentNode : contentArray) {
    //         var textNode = contentNode.get("text");
    //         if (textNode != null) {
    //           contents.add(new dev.langchain4j.data.message.TextContent(textNode.asText()));
    //         }
    //       }
    //     }
    //     return new UserMessage(contents);
    //   }
    // })
    ;
    mod.addDeserializer(Content.class, new JsonDeserializer<Content>() {
      @Override
      public Content deserialize(JsonParser p, DeserializationContext ctxt)
          throws IOException, JacksonException {
        // TODO Auto-generated method stub
        //return super.deserialize(p, ctxt, intoValue);
        var node = p.readValueAsTree();
        if (node.get("text") instanceof TextNode text) {
          return TextContent.from(text.asText());
        }
        return null;
      }
    });
    MAPPER.registerModule(mod);

    MAPPER.addMixIn(UserMessage.class, UserMessageMixIn.class);
    MAPPER.addMixIn(UserMessage.Builder.class, ThirdPartyBuilderMixIn.class);

  }

@JsonDeserialize(builder = UserMessage.Builder.class)
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@JsonIncludeProperties("content")
public abstract class UserMessageMixIn { 

  @JsonCreator
  static UserMessage create(Object ignored) {
    return null;
  } // only to attach annotation via mix-in; body ignored

}


@JsonPOJOBuilder(withPrefix = "")
public abstract class ThirdPartyBuilderMixIn {
    // Optional: if builder has a different method name for build, annotate it:
    //@JsonPOJOBuilder(buildMethodName = "build");

}

  public static class Messages {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
    public ArrayList<ChatMessage> messages;

    @JsonCreator
    public Messages(@JsonProperty("messages") ArrayList<ChatMessage> messages) {
      this.messages = new ArrayList<>(messages);
    }
  }

  public static List<ChatMessage> read(String json) {
    try {
      return MAPPER.readValue(json, Messages.class).messages;
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize messages", e);
    }
  }

  public static String write(List<ChatMessage> messages) {
    try {
      Messages msgs = new Messages(new ArrayList<>(messages));
      var raw = MAPPER2.writeValueAsString(msgs);
      return raw;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize messages", e);
    }
  }
}