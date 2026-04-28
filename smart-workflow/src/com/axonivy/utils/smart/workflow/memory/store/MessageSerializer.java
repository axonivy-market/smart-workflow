package com.axonivy.utils.smart.workflow.memory.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;

public class MessageSerializer {

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
    mod.addDeserializer(UserMessage.class, new JsonDeserializer<UserMessage>() {
      @Override
      public UserMessage deserialize(com.fasterxml.jackson.core.JsonParser p,
          DeserializationContext ctxt) throws IOException, JsonMappingException {
        var node = p.readValueAsTree();
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
        if (node.get("contents") instanceof ArrayNode contentArray) {
          for (var contentNode : contentArray) {
            var textNode = contentNode.get("text");
            if (textNode != null) {
              contents.add(new dev.langchain4j.data.message.TextContent(textNode.asText()));
            }
          }
        }
        return new UserMessage(contents);
      }
    })
    ;
    MAPPER.registerModule(mod);

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
      var raw = Json.toJson(msgs);
      return raw;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize messages", e);
    }
  }
}