package com.axonivy.utils.smart.workflow.memory.store;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class BusinessDataMemory implements ChatMemoryStore {

  @Override
  public void deleteMessages(Object id) {
    verifyId(id);
    Ivy.repo().deleteById((String) id);
  }

  @Override
  public List<ChatMessage> getMessages(Object id) {
    return findMemory(id)
        .map(ChatMemory::readMessages)
        .orElse(List.of());
  }

  @Override
  public void updateMessages(Object id, List<ChatMessage> messages) {
    var existing = findMemory(id);
    if (existing.isPresent()) {
      ChatMemory memory = existing.get();
      memory.setMessages(messages);
      Ivy.repo().overwrite(memory, "messages");
      return;
    }
    var memory = ChatMemory.of((String) id, messages);
    Ivy.repo().save(memory);
  }

  private Optional<ChatMemory> findMemory(Object id) {
    verifyId(id);
    return Optional.ofNullable(Ivy.repo().find((String) id, ChatMemory.class));
  }

  private static void verifyId(Object id) {
    if (!(id instanceof String)) {
      throw new IllegalArgumentException("Only String ids are supported");
    }
  }

  static class ChatMemory {

    public String id;
    public List<String> messages;

    ChatMemory(@JsonProperty("id") String id, @JsonProperty("messages") List<String> messages) {
      this.id = id;
      this.messages = messages;
    }

    static ChatMemory of(String id, List<ChatMessage> messages) {
      return new ChatMemory(id, write(messages));
    }

    private static List<String> write(List<ChatMessage> messages) {
      return messages.stream()
          .map(ChatMessageSerializer::messageToJson)
          .toList();
    }

    void setMessages(List<ChatMessage> messages) {
      this.messages = write(messages);
    }

    List<ChatMessage> readMessages() {
      return messages.stream()
          .map(ChatMessageDeserializer::messageFromJson)
          .toList();
    }
  }
}
