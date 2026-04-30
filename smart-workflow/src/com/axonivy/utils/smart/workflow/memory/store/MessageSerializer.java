package com.axonivy.utils.smart.workflow.memory.store;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;

public class MessageSerializer {

  public static List<ChatMessage> read(String json) {
    try {
      return ChatMessageDeserializer.messagesFromJson(json);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to deserialize messages", ex);
    }
  }

  public static String write(List<ChatMessage> messages) {
    try {
      return ChatMessageSerializer.messagesToJson(messages);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to serialize messages", ex);
    }
  }
}
