package com.axonivy.utils.ai.memory;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.memory.ChatMemory;

public class AgentMemory implements ChatMemory {

  private Object id;
  private List<ChatMessage> messages;
  private List<String> persistedMessages;

  public AgentMemory() {}

  public AgentMemory(Object memoryId) {
    this.id = memoryId;
    this.messages = new ArrayList<>();
  }

  @Override
  public Object id() {
    return id;
  }

  @Override
  public void add(ChatMessage message) {
    messages.add(message);
  }

  @Override
  public List<ChatMessage> messages() {
    return messages;
  }

  @Override
  public void clear() {
    messages.clear();
  }

  public List<String> getPersistedMessages() {
    return persistedMessages;
  }

  public void setPersistedMessages(List<String> persistedMessages) {
    this.persistedMessages = persistedMessages;
  }

  public void convertToPersistedMessages() {
    persistedMessages = new ArrayList<>();
    for (ChatMessage message : messages) {
      persistedMessages.add(Json.toJson(message));
    }
  }
}
